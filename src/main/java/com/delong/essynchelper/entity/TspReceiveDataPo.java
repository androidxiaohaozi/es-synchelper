package com.delong.essynchelper.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * @Author wh
 * @Date 2023/6/12 8:50
 * @Describe
 */
@Data
@Document(indexName = "tspreceivedata")
public class TspReceiveDataPo {

    @Id
    private Integer id;

    private String dataTime;

    private String a34001;

    private String a34002;

    private String a34004;

    private String MN;

    private String monitorPoints;

    private String monitorType;
}
