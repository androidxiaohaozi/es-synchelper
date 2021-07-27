package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.ApplyPo;
import com.delong.essynchelper.entity.CheckResult;
import com.delong.essynchelper.util.JsonUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PolicyCheckHandler extends TextWebSocketHandler implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(PolicyCheckHandler.class);

    @Resource
    @Qualifier("policyJdbcTemplate")
    private JdbcTemplate policyJdbcTemplate;

    @Resource
    private RestHighLevelClient restHighLevelClient;

    private String policySql;

    @Value("policy.log.filepath")
    private String logFilePath;

    @Value("${policy.log.check-field}")
    private String checkField;

    private String[] checkFields = null;

    //每个文件校验条数
    @Value("${policy.log.check-size}")
    private int checkSize;

    //每个文件的最大行数
    @Value("${policy.log.max-line}")
    private int maxLine;

    public PolicyCheckHandler() {
        policySql = getPolicySql();
        policySql += " where m.actualId=?";
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        HashMap map = JsonUtil.fromJson(payload, HashMap.class);
        Long jobId = Long.valueOf((String) map.get("jobId"));
        Integer recordCount = (Integer) map.get("recordCount");

        CheckResult result = new CheckResult();
        result.setJobId(jobId);
        result.setRecordCount(recordCount);
        int checkCount = recordCount / maxLine * checkSize;
        int last = recordCount % maxLine;
        if (last > checkSize) {
            checkCount += checkSize;
        } else {
            checkCount += last;
        }
        result.setCheckCount(checkCount);
        result.setStatus("校验中");
        result.setMessage("开始校验");
//        PolicyESVO vo1=getFromDB(2257048L);
//        PolicyESVO vo2=getFromES("22570482257004");
        session.sendMessage(new TextMessage(JsonUtil.toJson(result)));
        check(session, result);
    }

    private ApplyPo getFromDB(Long applyId) {
        return policyJdbcTemplate.queryForObject(policySql, new Object[]{applyId},
                new BeanPropertyRowMapper<>(ApplyPo.class));
    }

    private ApplyPo getFromES(String indexId) throws IOException {
//        Get get = new Get.Builder("contractcenter", ""+indexId).type("policyESVO").build();
//        JestResult result = jestClient.execute(get);

        GetRequest getRequest = new GetRequest("saapply", indexId);
        GetResponse documentFields = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

        String sourceAsString = documentFields.getSourceAsString();

        return JsonUtil.fromJson(sourceAsString, ApplyPo.class);

    }

    private String getPolicySql() {
        ClassPathResource classPathResource = new ClassPathResource("queryPolicy.sql");
        try (InputStream inputStream = classPathResource.getInputStream()) {
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            return new String(bytes);
        } catch (IOException e) {
            logger.error("找不到sql配置文件[classpath:queryPolicy.sql]", e);
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() {
        checkFields = checkField.split(",");
    }

    private void check(WebSocketSession session, CheckResult result) throws IOException {
        Long jobId = result.getJobId();
        File dir = new File(logFilePath + File.separator + jobId);
        if (!dir.exists()) {
            result.setStatus("校验失败");
            result.setMessage(dir.getPath() + "文件不存在，校验失败");
        }
        List<File> files = Arrays.asList(Objects.requireNonNull(dir.listFiles()));
        //按文件名排序
        files.sort(Comparator.comparing(File::getName));
        List<Long> acutals = getRandomLines(files, result.getCheckCount());
        result.setMessage(null);
        for (Long id : acutals) {
            ApplyPo vo1 = getFromDB(id);
            ApplyPo vo2 = getFromES(vo1.getApplyId().toString());
            boolean isSame = checkPolicy(vo1, vo2);
            if (isSame) {
                result.setAppendMsg("通过， " + id + "， " + vo1.getApplyId());
                result.setSuccess(result.getSuccess() + 1);
            } else {
                result.setAppendMsg("不通过，" + id + "， " + vo1.getApplyId() +
                        "<p>db：" + JsonUtil.toJson(vo1) + "</p><p>es：" + JsonUtil.toJson(vo2) + "</p>");
                result.setFailed(result.getFailed() + 1);
            }
            session.sendMessage(new TextMessage(JsonUtil.toJson(result)));
        }
        if (result.getSuccess() == result.getCheckCount()) {
            result.setStatus("校验通过");
        } else {
            result.setStatus("校验不通过");
        }
        result.setAppendMsg("校验完毕，" + result.getStatus());
        session.sendMessage(new TextMessage(JsonUtil.toJson(result)));
    }

    private List<Long> getRandomLines(List<File> files, int count) {
        List<Long> actuals = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            int size = checkSize;
            if (i == files.size() - 1) {
                size = count - actuals.size();
            }
            List<Long> sub = randomAccess(files.get(i), size);
            actuals.addAll(sub);
        }
        return actuals;
    }

    private List<Long> randomAccess(File file, int size) {
        List<Long> actuals = new ArrayList<>();
        try {
            long fileLength = file.length();
            LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
            lineNumberReader.skip(fileLength);
            int lines = lineNumberReader.getLineNumber();
            lineNumberReader.close();
            lineNumberReader = new LineNumberReader(new FileReader(file));
            List<Integer> picks = getRandom(lines, size);
            for (Integer lineNum : picks) {
                lineNumberReader.setLineNumber(lineNum);
                String line = lineNumberReader.readLine();
                actuals.add(Long.valueOf(line));
            }
            lineNumberReader.close();
        } catch (Exception e) {
            logger.error("文件读取失败" + file.getPath(), e);
        }
        return actuals;
    }

    private List<Integer> getRandom(int count, int pick) {
        List<Integer> list = new ArrayList<Integer>();
        if (pick >= count) {
            for (int i = 0; i < count; i++) {
                list.add(i);
            }
        } else {
            Random random = new Random();
            Integer temp = random.nextInt(count);
            while (list.size() < pick) {
                if (!list.contains(temp)) {
                    list.add(temp);
                }
                temp = random.nextInt(count);
            }
        }
        Collections.sort(list);
        return list;
    }

    private boolean checkPolicy(ApplyPo db, ApplyPo es) {
        try {
            for (String fieldName : checkFields) {
                Field field = db.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object dbValue = field.get(db);
                Field fieldEs = es.getClass().getDeclaredField(fieldName);
                fieldEs.setAccessible(true);
                Object esValue = fieldEs.get(es);
                boolean sameValue = isSameValue(dbValue, esValue);
                if (!sameValue) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("", e);
            return false;
        }

        return true;
    }

    private boolean isSameValue(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null) {
            return false;
        }
        if (o2 == null) {
            return false;
        }
        if (o1 instanceof Date) {
            return DateUtils.isSameInstant((Date) o1, (Date) o2);
        }
        if (o1 instanceof String) {
            return o1.equals(o2);
        }
        if (o1 instanceof BigDecimal) {
            return ((BigDecimal) o1).compareTo((BigDecimal) o2) == 0;
        }
        return o1.toString().equals(o2.toString());
    }
}
