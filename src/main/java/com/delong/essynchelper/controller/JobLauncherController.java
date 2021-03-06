package com.delong.essynchelper.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
             result.put("message","????????????????????????????????????ID???"+jobExecution.getId());
             result.put("id",jobExecution.getId());
            
        } catch (JobInstanceAlreadyCompleteException ex) { // ???????????????job???????????????????????????????????????????????????
            logger.error(ex.getMessage());
            result.put("success", false);
            result.put("message", "????????????????????????????????????????????????????????????????????????????????????");
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
            result.put("message", "??????" + executionId + "?????????????????????????????????ID???" + id);
        } catch (Exception e) {
            logger.error("??????[{}]????????????", executionId, e);
            result.put("success", false);
            result.put("message", "??????" + executionId + "???????????????" + e.getMessage());
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
                result.put("message", "???????????????????????????ID???" + executionId);
            } else {
                result.put("message", "???????????????????????????ID???" + executionId);
            }
        } catch (Exception e) {
            logger.error("??????[{}]????????????", executionId, e);
            result.put("success", false);
            result.put("message", "??????" + executionId + "????????????" + e.getMessage());
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
                builder.addLong("time",System.currentTimeMillis());
                builder.addString("whereSql",whereSql);
                try {
                    logger.info("???????????????????????? {}",whereSql);
                    JobParameters jobParameters = builder.toJobParameters();
                    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
                    logger.info("??????????????????????????? {}",whereSql);
                }catch (Exception e) {
                    logger.error("???????????????????????? {}",whereSql,e);
                }
            }
            result.put("success", true);
            result.put("message", lines.size()+"??????????????????????????????");
        }catch (IOException e) {
            logger.error("?????????sql????????????[classpath:batchWhere.txt]",e);
            result.put("success", false);
            result.put("message","?????????sql????????????[classpath:batchWhere.txt]");
        }
        return result;
    }
}
