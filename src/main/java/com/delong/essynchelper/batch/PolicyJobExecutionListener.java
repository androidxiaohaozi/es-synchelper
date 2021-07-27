package com.delong.essynchelper.batch;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 
 * @author dell
 *
 */
@Service
public class PolicyJobExecutionListener implements JobExecutionListener {

    private Logger logger = LoggerFactory.getLogger(PolicyJobExecutionListener.class);

    /**
     * policyLogFileHelper 
     */
    @Autowired
    private PolicyLogFileHelper policyLogFileHelper;

    /**
     * job
     */
    @Autowired
    Job job;

    /**
     * simpleJobLauncher
     */
    @Autowired
    @Qualifier("simpleJobLauncher")
    JobLauncher jobLauncher;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName() + "-" + jobExecution.getId();
        logger.info("开始执行Job : " + jobName);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        StepExecution step = jobExecution.getStepExecutions().iterator().next();
        policyLogFileHelper.clear(step.getJobExecutionId());
        long time = (step.getEndTime().getTime() - step.getStartTime().getTime()) / 1000;
        logger.info("Job执行完毕: " + jobExecution.getJobInstance().getJobName() + "-" + jobExecution.getId() + ", 耗时："
                + time + "s, " + "ReadCount=" + step.getReadCount() + ", WriteCount=" + step.getWriteCount()
                + ", status=" + step.getStatus());
    }

    /**
     * @param whereSql whereSql
     * @param result result
     * @throws JobExecutionAlreadyRunningException JobExecutionAlreadyRunningException
     * @throws JobRestartException JobRestartException
     * @throws JobInstanceAlreadyCompleteException JobInstanceAlreadyCompleteException
     * @throws JobParametersInvalidException JobParametersInvalidException
     */
    public void batchJobs(String whereSql, Map<String, Object> result) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {

        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong("time", System.currentTimeMillis());
        builder.addString("whereSql", whereSql);
        JobParameters jobParameters = builder.toJobParameters();
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        result.put("success", true);
        result.put("message", "任务已开始后台执行，任务ID：" + jobExecution.getId());
        result.put("id", jobExecution.getId());
    }
}
