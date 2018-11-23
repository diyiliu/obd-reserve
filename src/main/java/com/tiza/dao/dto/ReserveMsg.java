package com.tiza.dao.dto;

import lombok.Data;

import java.util.Date;

/**
 * Description: ReserveMsg
 * Author: DIYILIU
 * Update: 2018-11-21 16:00
 */

@Data
public class ReserveMsg {

    private Long id;

    private String deviceId;

    private Date sendTime;

    private String sendHex;

    private Date respTime;

    private String respHex;

    /** 回应结果(0:失败;1:成功;) **/
    private Integer respResult;

    /** 状态(0:未执行;1:执行成功;2:执行失败;) **/
    private Integer status;
}
