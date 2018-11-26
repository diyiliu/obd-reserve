package com.tiza.bolt;

import backtype.storm.Config;
import backtype.storm.Constants;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.tiza.dao.ReserveMsgDao;
import com.tiza.dao.dto.ReserveMsg;
import com.tiza.util.JacksonUtil;
import com.tiza.util.JdbcUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.*;

/**
 * Description: KafkaTripBolt
 * Author: DIYILIU
 * Update: 2018-07-16 14:28
 */

@Slf4j
public class ReserveBolt extends BaseRichBolt {

    private OutputCollector collector;

    private Map<String, Long> onlineMap;

    private Map<String, ReserveMsg> sendMap;

    private Jedis jedis;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        onlineMap = new HashMap();
        sendMap = new HashMap();

        String redisHost = (String) map.get("redisHost");
        int redisPort = Integer.parseInt((String) map.get("redisPort"));
        String redisPwd = (String) map.get("redisPwd");

        JedisPool jedisPool = new JedisPool(redisHost, redisPort);
        jedis = jedisPool.getResource();
        if (StringUtils.isNotEmpty(redisPwd)) {
            jedis.auth(redisPwd);
        }
    }

    @Override
    public void execute(Tuple tuple) {
        collector.ack(tuple);
        // 刷新缓存
        if (tuple.getSourceComponent().equals(Constants.SYSTEM_COMPONENT_ID)) {
            sendMap.clear();
            List<ReserveMsg> msgList = ReserveMsgDao.freshReserve();
            log.info("发送指令, 队列长度[{}] ... ", msgList.size());

            for (ReserveMsg msg : msgList) {
                String device = msg.getDeviceId();
                if (onlineMap.containsKey(device)) {
                    long time = onlineMap.get(device);
                    if (System.currentTimeMillis() - time < 3 * 60 * 1000) {
                        try {
                            sendToGw(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    onlineMap.remove(device);
                }
            }

            return;
        }

        try {
            String kafkaMsg = tuple.getString(0);
            Map data = JacksonUtil.toObject(kafkaMsg, HashMap.class);
            int cmd = (int) data.get("cmd");
            String device = String.valueOf(data.get("id"));
            long time = (long) data.get("timestamp");
            if (cmd == 0x97) {

                onlineMap.put(device, time);
            } else if (cmd == 0x7F) {
                if (sendMap.containsKey(device)) {
                    ReserveMsg msg = sendMap.get(device);
                    long sendTime = msg.getSendTime().getTime();
                    if (time - sendTime < 2 * 60 * 1000) {

                        String content = (String) data.get("data");
                        if (content.length() != 30) {
                            log.info("数据异常:[{}]", content);
                            return;
                        }
                        sendMap.remove(device);

                        int result = 0, status = 2;
                        String rs = content.substring(24, 26);
                        if (rs.equals("00")) {
                            result = 1;
                            status = 1;
                        }

                        String sql = "update obd_reserve_cmd t set t.resp_time=? ,t.resp_hex=?, t.resp_result=?, t.status = ? where t.id=?";
                        JdbcUtil jdbcUtil = JdbcUtil.getInstance();
                        try (Connection connection = jdbcUtil.getConnection();
                             PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                            statement.setString(2, content);
                            statement.setInt(3, result);
                            statement.setInt(4, status);
                            statement.setLong(5, msg.getId());

                            statement.execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("数据源错误: [{}]", JacksonUtil.toJson(tuple));
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        Map<String, Object> map = new HashMap();
        //给当前bolt设置定时任务 5分钟
        map.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 300);

        return map;
    }

    private void sendToGw(ReserveMsg msg) throws Exception {
        String device = msg.getDeviceId();

        String address = jedis.get("obd:t:" + device);
        if (StringUtils.isEmpty(address)) {

            log.info("Redis中无法获取设备网关地址!");
            return;
        }
        msg.setSendTime(new Date());
        sendMap.put(device, msg);

        address = address.substring(address.indexOf(":") + 1, address.length() - 1);
        String url = "http://" + address + "/setup";

        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost();
        request.setURI(new URI(url));
        //设置参数
        List<NameValuePair> list = new ArrayList();
        list.add(new BasicNameValuePair("serial", String.valueOf(msg.getId())));
        list.add(new BasicNameValuePair("device", device));
        list.add(new BasicNameValuePair("cmd", "90"));
        list.add(new BasicNameValuePair("content", msg.getSendHex()));
        request.setEntity(new UrlEncodedFormEntity(list, Charset.forName("UTF-8")));

        // 连接时间
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        // 数据传输时间
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 3000);
        HttpResponse response =  client.execute(request);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        log.info("[{}]下发指令: {}", device, result);

        String sql = "update obd_reserve_cmd t set t.send_time=? where t.id=?";
        JdbcUtil jdbcUtil = JdbcUtil.getInstance();
        try (Connection connection = jdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.setLong(2, msg.getId());

            statement.execute();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
