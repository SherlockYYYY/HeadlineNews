package com.heima.kafka.sample;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * 生产者
 */
public class ProducerQuickStart {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //1.kafka的配置信息
        Properties properties = new Properties();
        //kafka的连接地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        //发送失败，失败的重试次数
        properties.put(ProducerConfig.RETRIES_CONFIG,5);
        //消息key的序列化器
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //消息value的序列化器
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");

        properties.put(ProducerConfig.ACKS_CONFIG,"all"); //0 1 all 默认1

        //重试次数
        properties.put(ProducerConfig.RETRIES_CONFIG,10);
        //消息压缩
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        //2.生产者对象
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(properties);

        for (int i = 0; i < 5; i++) {

        //封装发送的消息
            ProducerRecord<String, String> KVrecord = new ProducerRecord<String, String>("itcast-topic-input", "hello kafka");
            producer.send(KVrecord);
        }

        //3.同步发送消息 但是会阻塞 可以采用异步
//        RecordMetadata recordMetadata = producer.send(KVrecord).get();
//        System.out.println(recordMetadata.offset());


        //3.异步发送消息
//        producer.send(KVrecord, new Callback() {
//            @Override
//            public void onCompletion(RecordMetadata metadata, Exception exception) {
//                if(exception != null){
//                    System.out.println("记录异常消息到日志表");
//                }
//                System.out.println(metadata.offset());
//            }
//
//        });

        //4.关闭消息通道，必须关闭，否则消息发送不成功
        producer.close();
    }

}