package com.delong.essynchelper.entity;

import lombok.Data;

@Data
public class CheckResult {
    private Long jobId;

    private String status;

    private int recordCount;

    private int checkCount;

    private int success;

    private int failed;

    private String message;

    private String appendMsg;

}
