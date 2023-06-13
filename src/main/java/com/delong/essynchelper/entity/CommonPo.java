package com.delong.essynchelper.entity;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

/**
 * @Author wh
 * @Date 2023/6/5 16:36
 * @Describe
 */
@Data
@Document(indexName = "pellandfeeding")
public class CommonPo {

    private String ELECURVALUE;

    private String ELECURTIMESTAMP;

    @Id
    private Integer id;

    /**
     * 申请评价信息
     */
    private List<CommonPo> commonPoList;

/*    //多条件查询
    private String multiQueryIndex;*/

/*    *//**
     * 将常用的几个查询条件， 拼接成一个
     *//*
    public void setDefaultMultiQueryIndex() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNoneBlank(this.ELECURTIMESTAMP)) {
            sb.append(this.ELECURTIMESTAMP).append(" ");  // 时间加索引
        }
        this.multiQueryIndex = sb.toString();
    }*/
}
