package com.heima.kafka.sample;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaStreamQuickStart {
    public static void main(String[] args){
        //kafka的配置信息
        Properties prop = new Properties();
        prop.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.200.130:9092");
        prop.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        prop.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        //应用的id
        prop.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-quickstart");
        //stream构造器
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        //流式计算
        streamProcessor(streamsBuilder);

        try (KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), prop)) {
            //开启流式计算
                kafkaStreams.start();

        }
    }

    /**
     * 流处理
     * 消息的内容 hello kafka 之类的
     * @param streamsBuilder
     */
    private static void streamProcessor(StreamsBuilder streamsBuilder) {
        //创建stream对象，指定从哪个topic中接收消息
        KStream<String, String> stream = streamsBuilder.stream("itcast-topic-input");
        stream.flatMapValues(new ValueMapper<String, Iterable<String>>() {
            /**
             * value就是 topic中接收到消息
             * @param value
             * @return
             */
            @Override
            public Iterable<String> apply(String value) {  //value就是 topic中接收到的消息
                return Arrays.asList(value.split(" "));
            }
        })
                //按照value进行分组
                .groupBy((key, value) -> value)
                //10秒的滑动窗口 每十秒聚合一次
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                //统计单词的个数 按照value
                .count()
                //转换为kstream
                .toStream()
                .map((key, value) -> {
                    System.out.println(key.key() + ":" + value);
                    return new KeyValue<>(key.key(), value.toString());
                })
                .to("itcast-topic-out"); //发送到新的topic中
    }
}
