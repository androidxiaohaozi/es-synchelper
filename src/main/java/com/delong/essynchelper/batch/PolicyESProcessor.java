package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.ApplyPo;
import com.delong.essynchelper.entity.AppraisePo;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyESProcessor implements ItemProcessor<ApplyPo, ApplyPo> {

    @Autowired
    @Qualifier("policyJdbcTemplate")
    private JdbcTemplate policyJdbcTemplate;

    private String taskSql1 = "select * from t_sa_appraise where f_batch_relation_type = 0 and f_relation_id = ? " +
            "f_relation_type = 0 and f_is_delete = 1 ";

    /**
     * 数据处理，填充评价数据
     * @param applyPo a
     * @return ApplyPo
     * @throws Exception e
     */
    @Override
    public ApplyPo process(ApplyPo applyPo) {
        updateTask(applyPo);
//        updatePay(applyPo);
        applyPo.setDefaultMultiQueryIndex();
        return applyPo;
    }

    /**
     * 更新task表对应的数据
     * @param applyPo applyPo
     */
    private void updateTask(ApplyPo applyPo){
        try {
            List<AppraisePo> appraisePos = policyJdbcTemplate.queryForList(taskSql1, AppraisePo.class);
            applyPo.setApplAppraisePos(appraisePos);
        }catch (DataAccessException e){
            // do nothing,查询结果为null时会抛出异常，忽略掉
        }
   }

}
