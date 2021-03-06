package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.ApplyPo;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@StepScope
public class PolicyItemWriteListener implements ItemWriteListener<ApplyPo> {

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobId;

    @Autowired
    private PolicyLogFileHelper policyLogFileHelper;

    @Override
    public void beforeWrite(List<? extends ApplyPo> list) {

    }

    @Override
    public void afterWrite(List<? extends ApplyPo> list) {
        policyLogFileHelper.write(jobId,list);
    }

    @Override
    public void onWriteError(Exception e, List<? extends ApplyPo> list) {
//        System.out.println(jobId+" onWriteError");
//        e.printStackTrace();
    }
}
