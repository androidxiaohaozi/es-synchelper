package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.ApplyPo;
import com.delong.essynchelper.entity.CommonPo;
import com.delong.essynchelper.entity.TspReceiveDataPo;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@StepScope
public class PolicyItemWriteListener implements ItemWriteListener<TspReceiveDataPo> {

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobId;

    @Autowired
    private PolicyLogFileHelper policyLogFileHelper;

    @Override
    public void beforeWrite(List<? extends TspReceiveDataPo> list) {
        for (TspReceiveDataPo commonPo : list) {
            System.out.printf(commonPo.getMN());
        }
    }

    @Override
    public void afterWrite(List<? extends TspReceiveDataPo> list) {
        policyLogFileHelper.write(jobId,list);
    }

    @Override
    public void onWriteError(Exception e, List<? extends TspReceiveDataPo> list) {
//        System.out.println(jobId+" onWriteError");
//        e.printStackTrace();
    }
}
