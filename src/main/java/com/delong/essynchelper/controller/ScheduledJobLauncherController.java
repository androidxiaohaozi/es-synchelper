package com.delong.essynchelper.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableScheduling
public class ScheduledJobLauncherController implements InitializingBean {
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
    JdbcTemplate jdbcTemplate;

    String batchExecutionSql = "select * from (select t.*,p.string_val as where_sql from BATCH_STEP_EXECUTION t " +
            "left join BATCH_JOB_EXECUTION_PARAMS p on p.job_execution_id=t.job_execution_id and p.key_name='whereSql' " +
            "order by t.start_time desc) where rownum <= 50";


    @Scheduled(cron = "0 0 9 ? * *")
    @RequestMapping("/launchscheduledjob")
    public Map<String, Object> launchJob() throws Exception {
        Map<String, Object> result = new HashMap<>();
        String whereSql = "";
        JobParametersBuilder builder = null;
        try {
            builder = new JobParametersBuilder();
            whereSql = "where m.f_create_time >= trunc(sysdate-1)+6/24 and m.f_create_time <= trunc(sysdate)+9/24 and m.f_apply_id > 0 ";
            builder.addString("whereSql", whereSql);
            JobParameters jobParameters = builder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            result.put("success", true);
            result.put("message", "任务已开始后台执行，任务ID：" + jobExecution.getId());
            result.put("id", jobExecution.getId());
        } catch (JobInstanceAlreadyCompleteException ex) { //相同参数的job重复执行时会报错提示任务已经执行过
            builder.addLong("time", System.currentTimeMillis());
            whereSql = "where m.f_create_time >= trunc(sysdate-1)+6/24 and m.f_create_time <= trunc(sysdate)+9/24 and m.f_apply_id > 0 ";
            builder.addString("whereSql", whereSql);
            JobParameters jobParameters = builder.toJobParameters();
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            result.put("success", true);
            result.put("message", "任务已开始后台执行，任务ID：" + jobExecution.getId());
            result.put("id", jobExecution.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @RequestMapping("/restartScheduledJob")
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

    @RequestMapping("/stopScheduledJob")
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

    @RequestMapping("/searchScheduledExecution")
    public List<Map<String, Object>> searchExecution() {
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(batchExecutionSql);
        return maps;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jobOperator instanceof SimpleJobOperator) {
            SimpleJobOperator simpleJobOperator = (SimpleJobOperator) jobOperator;
            simpleJobOperator.setJobLauncher(jobLauncher);
            simpleJobOperator.setJobRepository(myJobRepository);
        }
    }
}
