package com.heima.kafka.sample;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * 消费者
 */
public class ConsumerQuickStart {

    public static void main(String[] args) {
        ///卡夫卡配置信息
        Properties properties = new Properties();
        //连接地址
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");

        //key value反序列化器  因为要通过网络传输
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        //设置消费者组
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"test-group");
        //关闭自动提交 手动提交  有同步 异步 同步+异步三种方式 因为异步失败之后不会重试
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);

        //2.创建消费者对象
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);
        //3.订阅主题
        consumer.subscribe(Collections.singletonList("topic01"));
        //4.拉取消息
        try {
            while(true){
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));//以一秒的频率拉取消息
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords){
                    System.out.println(consumerRecord.key());
                    System.out.println(consumerRecord.value());
                    System.out.println(consumerRecord.partition()); //获取消息存储在那个分区
                    //同一个topic中同一个分区的消息，是同一个offset 一个topic可以有不同的分区存储
                    System.out.println(consumerRecord.offset());//消费者消费到哪个位置了
                    //同步提交offset
                }
                //异步提交offset
                consumer.commitAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("记录同步提交offset失败异常"+e);
        }finally {
            //最终同步提交
            consumer.commitSync();
        }


//        while(true){
//            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));//以一秒的频率拉取消息
//            for (ConsumerRecord<String, String> consumerRecord : consumerRecords){
//                System.out.println(consumerRecord.key());
//                System.out.println(consumerRecord.value());
//                System.out.println(consumerRecord.partition()); //获取消息存储在那个分区
//                //同一个topic中同一个分区的消息，是同一个offset 一个topic可以有不同的分区存储
//                System.out.println(consumerRecord.offset());//消费者消费到哪个位置了
                //同步提交偏移量offset
//                try{
//                    consumer.commitSync();
//                } catch (Exception e) {
//                    System.out.println("记录同步提交offset失败异常");
//                    e.printStackTrace();
//                }

//            }
            //异步提交offset
//            consumer.commitAsync(new OffsetCommitCallback() {
//                @Override
//                public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
//                    if(exception != null){
//                        System.out.println("记录异步提交offset失败异常"+offsets+ exception);
//                    }
//                }
//            });

            //同步异步一起用

//        }
    }

}