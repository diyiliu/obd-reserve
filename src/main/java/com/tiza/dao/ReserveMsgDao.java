package com.tiza.dao;

import com.tiza.dao.dto.ReserveMsg;
import com.tiza.util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: ReserveMsgDao
 * Author: DIYILIU
 * Update: 2018-11-21 16:02
 */


public class ReserveMsgDao {
    public static List<ReserveMsg> freshReserve(){
        List<ReserveMsg> list = new ArrayList();

        String sql = "SELECT t.id," +
                     " t.device_id deviceid," +
                     " t.send_hex sendhex" +
                     " FROM" +
                     "     obd_reserve_cmd t" +
                     " WHERE" +
                     "     t.`status` = 0";

        JdbcUtil jdbcUtil = JdbcUtil.getInstance();
        try(Connection connection = jdbcUtil.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()) {

            while (rs.next()){
                long id = rs.getLong("id");
                String deviceId = rs.getString("deviceid");
                String sendHex = rs.getString("sendhex");

                ReserveMsg msg = new ReserveMsg();
                msg.setId(id);
                msg.setDeviceId(deviceId);
                msg.setSendHex(sendHex);
                list.add(msg);
            }
        }catch (Exception e){

            e.printStackTrace();
        }

        return list;
    }
}
