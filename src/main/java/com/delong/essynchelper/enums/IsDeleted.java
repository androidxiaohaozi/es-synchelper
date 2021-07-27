package com.delong.essynchelper.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum IsDeleted {
    DELETED(0,"已删除"),
    NOTDELETED(1,"未删除");

    private Integer code;
    private String display;
    IsDeleted(Integer code,String display){
        this.code = code;
        this.display = display;
    }
    private static final Map<Integer,IsDeleted> codeToEnum = new HashMap<>();
    private static final Map<Integer,String> enumMap = new HashMap<>();

    static {
        for (IsDeleted isDeleted : IsDeleted.values()){
            codeToEnum.put(isDeleted.getCode(),isDeleted);
            enumMap.put(isDeleted.getCode(),isDeleted.getDisplay());
        }
    }
    /**
     * 功能描述: 获取是否删除的枚举
     * @Param: [code]
     * @Return: com.dl.cihong.enums.IsDeleted
     * @Author: lvyy
     * @Date: 2020/10/20
     */
    public static IsDeleted valueOf(Integer code){
        return codeToEnum.get(code);
    }

    public Integer getCode() {
        return code;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    @JsonValue
    public String toString() {
        return display.intern().equalsIgnoreCase("已删除")?"是":"否";
    }
}
