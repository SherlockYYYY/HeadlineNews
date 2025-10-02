package com.heima.kafka.stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@Slf4j
public class KafkaStreamHelloListener {

    @Bean
    public KStream<String, String> KStream(StreamsBuilder streamsBuilder) {
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
        return stream;
    }

}
