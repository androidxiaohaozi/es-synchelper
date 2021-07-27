package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.ApplyPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@StepScope
public class PolicySkipListener implements SkipListener<ApplyPo, ApplyPo> {

    private Logger logger = LoggerFactory.getLogger("skipLog");

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobId;

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    @Override
    public void onSkipInRead(Throwable throwable) {

    }

    /**
     * 记录出错跳过的申请数据jobId,applyid,batchid
     * @param policyESVO policyESVO
     * @param throwable throwable
     */
    @Override
    public void onSkipInWrite(ApplyPo policyESVO, Throwable throwable) {
        logger.info("{},{},{}", jobId, policyESVO.getApplyId(), policyESVO.getBatchId());
    }

    @Override
    public void onSkipInProcess(ApplyPo policyESVO, Throwable throwable) {

    }
}
