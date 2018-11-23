package com.tiza;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;
import com.tiza.bolt.ReserveBolt;
import com.tiza.util.JacksonUtil;
import org.apache.commons.cli.*;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;

import java.io.InputStream;
import java.util.Properties;

/**
 * Description: Main
 * Author: DIYILIU
 * Update: 2018-11-21 14:42
 */
public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println(JacksonUtil.toJson(args));
        Options options = new Options();
        Option subOp = new Option("l", "local", true, "local or cluster[0: cluster,1: local]");
        subOp.setRequired(true);
        options.addOption(subOp);

        CommandLineParser parser = new PosixParser();
        CommandLine cli = parser.parse(options, args);

        Properties properties = new Properties();
        try (InputStream in = ClassLoader.getSystemResourceAsStream("obd.properties")) {
            properties.load(in);
            String topic = properties.getProperty("kafka.topic");
            ZkHosts zkHosts = new ZkHosts(properties.getProperty("kafka.zk-host"));

            SpoutConfig spoutConfig = new SpoutConfig(zkHosts, topic, "", "reserve");
            spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());

            TopologyBuilder builder = new TopologyBuilder();
            builder.setSpout("spout", new KafkaSpout(spoutConfig), 1);
            builder.setBolt("bolt1", new ReserveBolt(), 1).shuffleGrouping("spout");

            Config conf = new Config();
            conf.setDebug(false);
            conf.put(backtype.storm.Config.TOPOLOGY_MAX_SPOUT_PENDING, 64);   //限流
            conf.put("redisHost", properties.getProperty("redis.host"));
            conf.put("redisPort", properties.getProperty("redis.port"));
            conf.put("redisPwd", properties.getProperty("redis.password"));

            // 本地模式 + 集群模式
            if (cli.getOptionValue("local").equals("1")) {
                LocalCluster localCluster = new LocalCluster();
                localCluster.submitTopology("obd_reserve", conf, builder.createTopology());
            } else {
                StormSubmitter.submitTopology("obd_reserve", conf, builder.createTopology());
            }
        }
    }
}
