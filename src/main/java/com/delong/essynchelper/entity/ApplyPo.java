package com.delong.essynchelper.entity;

import com.delong.essynchelper.enums.IsDeleted;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * t_sa_apply
 * @author wanghao
 */
@Data
@Document(indexName = "saapply")
public class ApplyPo implements Serializable {


    /** 索引Id */
    @Id
    private Long applyId;

    /**
     * 批次号
     */
    private String batchId;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 县
     */
    private String county;

    /**
     * 地址
     */
    private String address;

    /**
     * 地址code
     */
    private String addressCode;

    /**
     * 学校ID
     */
    private Integer schoolId;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 提交日期
     */
    private Date submitTime;

    /**
     * 捐赠方
     */
    private String donors;

    /**
     * 是否资助（1-是，0-否）
     */
    private Integer supported;

    /**
     * 状态(0已开始,1已结束)
     */
    private Integer status;

    /**
     * 删除状态(0-删除,1-未删)
     */
    private IsDeleted deleted;


    /**
     * 备注(驳回必填)
     */
    private String remarks;

    /**
     * 审批状态(0为拒接状态,1为审批中,2为编辑中,3审核通过)
     */
    private Integer applStatus;

    /**
     * 审批状态(0为拒接状态,1为审批中,2为编辑中,3审核通过) 字符串描述 20210222调整
     */
    private String applStatusDesc;

    /**
     * 活动总结审批状态
     */
    private Integer sumStatus;

    /**
     * 活动总结审批状态 字符串描述 20210222调整
     */
    private String sumStatusDesc;

    /**
     * 学校userId
     */
    private Long schoolUserId;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 修改时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date updateTime;

    /**
     * 修改人
     */
    private String updateUser;

    /**
     * 申请重新提交标识
     */
    private Integer reSubmitFlag;

    /**
     * 申请序号
     */
    private String applNeeds;

    /**
     * 申请评价信息
     */
    private List<AppraisePo> ApplAppraisePos;

    //多条件查询
    private String multiQueryIndex;

    /**
     * 将常用的几个查询条件， 拼接成一个
     */
    public void setDefaultMultiQueryIndex() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNoneBlank(this.schoolName)) {
            sb.append(this.schoolName).append(" ");  // 学校姓名
        }
        if (StringUtils.isNoneBlank(this.applStatus.toString())) {
            sb.append(this.applStatus.toString()).append(" "); // 审核状态
        }
        if (StringUtils.isNoneBlank(this.address)) {
            sb.append(this.address).append(" ");  // 地址
        }

        this.multiQueryIndex = sb.toString();
    }
    private static final long serialVersionUID = 1L;
}