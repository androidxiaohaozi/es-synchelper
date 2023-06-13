package com.delong.essynchelper.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobLauncherController implements InitializingBean {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    @Qualifier("simpleJobLauncher")
    JobLauncher jobLauncher;

    @Autowired
    Job job;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier("myJobRepository")
    private JobRepository myJobRepository;

    @Autowired
    @Qualifier("policyJdbcTemplate")
    private JdbcTemplate policyJdbcTemplate;

    @Autowired
    private RestHighLevelClient client;

    String batchExecutionSql = "select * from (select t.*,p.string_val as where_sql from BATCH_STEP_EXECUTION t "
            + "left join BATCH_JOB_EXECUTION_PARAMS p on p.job_execution_id=t.job_execution_id and p.key_name='whereSql' "
            + "order by t.start_time desc) as a limit 200";

    
    public static int newpolicynum = 0;
    public static int oldpolicynum = 0;
    public static int negativenum = 0;
    
    @RequestMapping("/launchjob")
    public Map<String, Object> launchJob(@RequestParam(required = false) String whereSql,
            @RequestParam(required = false) boolean force) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {

             JobParametersBuilder builder =new JobParametersBuilder();
             if (force){
             builder.addLong("time",System.currentTimeMillis());
             }
             builder.addString("whereSql",whereSql);
             JobParameters jobParameters = builder.toJobParameters();
             JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            
             result.put("success",true);
             result.put("message","任务已开始后台执行，任务ID："+jobExecution.getId());
             result.put("id",jobExecution.getId());
            
        } catch (JobInstanceAlreadyCompleteException ex) { // 相同参数的job重复执行时会报错提示任务已经执行过
            logger.error(ex.getMessage());
            result.put("success", false);
            result.put("message", "一个相同参数的任务已执行完毕，如需重复执行请选择强制执行");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @RequestMapping("/restartJob")
    public Map<String, Object> restartJob(@RequestParam Long executionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long id = jobOperator.restart(executionId);
            result.put("success", true);
            result.put("message", "任务" + executionId + "已开始继续执行，新任务ID：" + id);
        } catch (Exception e) {
            logger.error("任务[{}]重启失败", executionId, e);
            result.put("success", false);
            result.put("message", "任务" + executionId + "重启失败，" + e.getMessage());
        }
        return result;
    }

    @RequestMapping("/stopJob")
    public Map<String, Object> stopJob(@RequestParam Long executionId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean stop = jobOperator.stop(executionId);
            result.put("success", true);
            if (stop) {
                result.put("message", "任务终止成功，任务ID：" + executionId);
            } else {
                result.put("message", "任务终止失败，任务ID：" + executionId);
            }
        } catch (Exception e) {
            logger.error("任务[{}]终止失败", executionId, e);
            result.put("success", false);
            result.put("message", "任务" + executionId + "终止失败" + e.getMessage());
        }
        return result;
    }

    @RequestMapping("/searchExecution")
    public List<Map<String, Object>> searchExecution() {
        List<Map<String, Object>> maps = policyJdbcTemplate.queryForList(batchExecutionSql);
        return maps;
    }

    @Override
    public void afterPropertiesSet() {
        if (jobOperator instanceof SimpleJobOperator) {
            SimpleJobOperator simpleJobOperator = (SimpleJobOperator) jobOperator;
            simpleJobOperator.setJobLauncher(jobLauncher);
            simpleJobOperator.setJobRepository(myJobRepository);
        }
    }


    @RequestMapping("/launchBatchJob")
    public Map<String, Object> launchBatchJob() {
        Map<String, Object> result = new HashMap<>();
        ClassPathResource classPathResource = new ClassPathResource("batchWhere.txt");
        try (InputStream inputStream = classPathResource.getInputStream()){
            List<String> lines = IOUtils.readLines(inputStream, "utf-8");
            for (String whereSql : lines){
                JobParametersBuilder builder =new JobParametersBuilder();
//                builder.addLong("time",System.currentTimeMillis());
                builder.addString("whereSql",whereSql);
                try {
                    logger.info("批量任务准备执行 {}",whereSql);
                    JobParameters jobParameters = builder.toJobParameters();
                    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                    logger.info("批量任务已开始运行 {}",whereSql);
                }catch (Exception e) {
                    logger.error("批量任务启动失败 {}",whereSql,e);
                }
            }
            result.put("success", true);
            result.put("message", lines.size()+"个批量任务已开始执行");
        }catch (IOException e) {
            logger.error("找不到sql配置文件[classpath:batchWhere.txt]",e);
            result.put("success", false);
            result.put("message","找不到sql配置文件[classpath:batchWhere.txt]");
        }
        return result;
    }

    @RequestMapping("/testSearch")
    public void testSearch(String type) {

        try {
            SearchRequest searchRequest = new SearchRequest("pellandfeeding");
            SearchSourceBuilder searchSourceBuilder;

            if ("1".equals(type)) {
                searchSourceBuilder = new SearchSourceBuilder().sort("ELECURTIMESTAMP", SortOrder.ASC);
            } else {
                searchSourceBuilder = new SearchSourceBuilder().sort("ELECURTIMESTAMP", SortOrder.DESC);
            }

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

            boolQueryBuilder.must(QueryBuilders.rangeQuery("ELECURTIMESTAMP").gte("2023-06-05 11:56:28").lte("2023-06-05 16:01:28"));

            searchSourceBuilder.query(boolQueryBuilder);

            searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

            searchRequest.source(searchSourceBuilder);

            //不分页查询
            searchSourceBuilder.size(2);
            Scroll scroll = new Scroll(TimeValue.timeValueMillis(1L));
            searchRequest.scroll(scroll);

            SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);

            String scrollId = search.getScrollId();
            SearchHit[] hits = search.getHits().getHits();
            List<SearchHit> resultSearchHit = new ArrayList<>();
            while (hits != null && hits.length > 0) {
                resultSearchHit.addAll(Arrays.asList(hits));
                SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
                searchScrollRequest.scroll(scroll);
                SearchResponse searchScrollResponse = client.scroll(searchScrollRequest, RequestOptions.DEFAULT);
                scrollId = searchScrollResponse.getScrollId();
                hits = searchScrollResponse.getHits().getHits();
            }
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            client.clearScroll(clearScrollRequest,RequestOptions.DEFAULT);

            if (resultSearchHit.size() > 0) {
                for (SearchHit hit : resultSearchHit) {
                    Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    System.out.println(sourceAsMap);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
