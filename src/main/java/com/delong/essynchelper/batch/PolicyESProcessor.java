package com.delong.essynchelper.batch;

import com.delong.essynchelper.entity.CommonPo;
import com.delong.essynchelper.entity.TspReceiveDataPo;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyESProcessor implements ItemProcessor<TspReceiveDataPo, TspReceiveDataPo> {

    @Autowired
    @Qualifier("policyJdbcTemplate")
    private JdbcTemplate policyJdbcTemplate;

    private String taskSql1 = "select * from t_pellandfeeding";

    /**
     * 数据处理，填充评价数据
     * @param applyPo a
     * @return ApplyPo
     * @throws Exception e
     */
    @Override
    public TspReceiveDataPo process(TspReceiveDataPo applyPo) {
//        updateTask(applyPo);
//        updatePay(applyPo);
//        applyPo.setDefaultMultiQueryIndex();
        return applyPo;
    }

    /**
     * 更新task表对应的数据
     * @param applyPo applyPo
     */
    private void updateTask(CommonPo applyPo){
        try {
            List<CommonPo> appraisePos = policyJdbcTemplate.queryForList(taskSql1, CommonPo.class);
            applyPo.setCommonPoList(appraisePos);
        }catch (DataAccessException e){
            // do nothing,查询结果为null时会抛出异常，忽略掉
        }
   }

}
