package com.delong.essynchelper.entity;

/**
 * @Description: 投保单状态
 * @date: 2017年8月2日
 * @author: huangbiao
 * @version: 1.0.0
 * @Copyright: 2017 www.chinalife-p.com.cn Inc. All rights reserved.
 */
public enum BusinessStatusEnum {

    /**
     * init
     */
    INIT("01", "Init", "初始化"),
    /**
     * entering
     */
    ENTERING("02", "Entering", "录入中"),
    /**
     * reject
     */
    REJECT("03", "Reject", "退回修改"),
    /**
     * auditing
     */
    WAITAUDIT("15", "waitAudit", "待审核"),
    /**
     * auditing
     */
    AUDITING("04", "Auditing", "审核中"),
    /**
     * audited
     */
    AUDITED("05", "Audited", "审核通过"),

    /**
     * signed
     */
    SIGNED("06", "Signed", "签发"),
    /**
     * repeal
     */
    REPEAL("10", "Repeal", "撤单"),
    /**
     * decline
     */
    DECLINE("11", "Decline", "拒保"),
    /**
     * retreat
     */
    RETREAT("12", "Retreat", "全单退保"),
    /**
     * cancel
     */
    CANCEL("13", "Cancel", "保单注销"),
    /**
     * Temp
     */
    TEMP("00", "Temp", "临时版本"),
    
    /**
     * SUSPEND 中止的意思是保单暂停，不是终止！
     */
    SUSPEND("14", "Suspend", "中止"),

    /**
     * cancel
     */
    ENDORSE_CANCEL("15", "endorseCancel", "批单注销");
    
    /**
     * code
     */
    private String code;
    /**
     * name
     */
    private String name;
    /**
     * description
     */
    private String description;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Contractor
     *
     * @param code        code
     * @param name        name
     * @param description description
     */
    BusinessStatusEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * @param code code
     *
     * @return 枚举类对象
     *
     * @Description: 通过value获取PolicyStateConstant对象
     */
    public static BusinessStatusEnum stateOf(String code) {
        for (BusinessStatusEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

    /**
     * is reject
     *
     * @return boolean
     */
    public boolean isReject() {
        return REJECT.getCode().equals(this.code);
    }

    /**
     * is signed
     *
     * @return boolean
     */
    public boolean isSigned() {
        return SIGNED.getCode().equals(this.getCode());
    }
}
