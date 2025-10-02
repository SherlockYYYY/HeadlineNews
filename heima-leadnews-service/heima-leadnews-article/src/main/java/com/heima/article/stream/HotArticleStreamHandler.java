package com.heima.article.stream;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.UpdateArticleMess;
import com.mysql.jdbc.UpdatableResultSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class HotArticleStreamHandler {

    @Bean
    public KStream<String, String> KStream(StreamsBuilder streamsBuilder) {
        //接受消息
        KStream<String, String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);
        //聚合流式数据  value是消息的值
        stream.map((key, value) -> {
            UpdateArticleMess updateArticleMess = JSON.parseObject(value, UpdateArticleMess.class);
            //重置消息的key和value  add是1就加 是-1就减
            //key是文章的 id value是文章的行为
            return new KeyValue<>(updateArticleMess.getArticleId().toString(), updateArticleMess.getType().name()+":"+updateArticleMess.getAdd());
        })
                //分组 用文章id聚合
                .groupBy((key, value) -> key)
                //多长时间聚合一次  10秒 不至于每次对文章一点赞就会更新数据库
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                //自行完成聚合计算
                .aggregate(new Initializer<String>() {
                    /**
                     * 初始方法 返回值是消息的value
                     * @return
                     */
                    @Override
                    public String apply() {
                        return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                    }
                }, new Aggregator<String, String, String>() {
                    /**
                     * 真正的聚合操作
                     * @param key
                     * @param value
                     * @param aggValue
                     * @return
                     */
                    //aggValue是第一个apply的返回值
                    @Override
                    public String apply(String key, String value, String aggValue) {
                        //KEY 是文章的id value是文章行为
                        if(StringUtils.isBlank( value)) return aggValue;
                        String[] aggAry = aggValue.split(",");

                        int col = 0,com = 0,like = 0,view = 0;
                        for (String agg : aggAry) {
                            String[] split = agg.split(":");
                            /*
                            获得初始值 并且在时间窗口内计算得到值并更新
                             */
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                                case COLLECTION:
                                    col = Integer.parseInt(split[1]);
                                    break;
                                case COMMENT:
                                    com = Integer.parseInt(split[1]);
                                    break;
                                case LIKES:
                                    like = Integer.parseInt(split[1]);
                                    break;
                                case VIEWS:
                                    view = Integer.parseInt(split[1]);
                                    break;
                            }
                        }
                        //累加操作 把计算出来的数值加上去
                        String[] valAry = value.split(":"); //valAry[0]是文章行为 valAry[1]是正负1
                        switch (UpdateArticleMess.UpdateArticleType.valueOf(valAry[0])){
                            case COLLECTION:
                                col += Integer.parseInt(valAry[1]);
                                break;
                            case COMMENT:
                                com += Integer.parseInt(valAry[1]);
                                break;
                            case LIKES:
                                like += Integer.parseInt(valAry[1]);
                                break;
                            case VIEWS:
                                view += Integer.parseInt(valAry[1]);
                                break;
                        }
                        String formatStr = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", col, com, like, view);
                        System.out.println("文章id:" + key);
                        System.out.println("当前时间窗口内的消息处理结果:"+formatStr);
                        return formatStr;

                    }
                }, Materialized.as("hot-article-stream-count-001"))
                .toStream()
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);
                //Materialized.as是创建一个临时的表，可能很多流程，起个名字
        return stream;

    }
}
