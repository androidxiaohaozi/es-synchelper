package com.delong.essynchelper.entity;

import com.delong.essynchelper.enums.IsDeleted;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * t_sa_appraise
 * @author wanghao
 */
@Data
public class AppraisePo implements Serializable {
    /**
     * 主键ID
     */
    private Long appraiseId;

    /**
     * 关联表ID
     */
    private String relationId;

    /**
     * 关联表类型(0-申请表，1总结表)
     */
    private Integer relationType;

    /**
     * 批次关联评价管理类型(0-申请表，1-总结表)
     */
    private Integer batchRelationType;

    /**
     * 评价详情关联批次对应的评价ID
     */
    private Long apprBatchRelaId;

    /**
     * 评价标题
     */
    private String apprTitle;
    /**
     * 评价内容
     */
    private String apprContent;

    /**
     * 评价索引
     */
    private Boolean apprIndex;

    /**
     * 创建人
     */
    private String createUser;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改人
     */
    private String updateUser;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 是否删除，0删除，1不删除
     */
    private IsDeleted deleted;

    /**
     * 打分 5分制
     */
    private Integer scoring;

    private static final long serialVersionUID = 1L;
}