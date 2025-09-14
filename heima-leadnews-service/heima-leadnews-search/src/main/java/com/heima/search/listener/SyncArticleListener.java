package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.search.vos.SearchArticleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SyncArticleListener {
    @Autowired
    private RestHighLevelClient restHighLevelClient;      // es客户端
    @KafkaListener(topics = ArticleConstants.ARTICLE_ES_SYNC_TOPIC)
    public void onMessage(String message) throws IOException {

        if(StringUtils.isNotBlank(message)){

            SearchArticleVo searchArticleVo = JSON.parseObject(message, SearchArticleVo.class);
            IndexRequest indexRequest = new IndexRequest("app_info_article");
            indexRequest.id(searchArticleVo.getId().toString()); //设置索引的id与文章id一致
            indexRequest.source(message, XContentType.JSON);  //message刚好是json字符串 所以直接用message
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            log.info("SyncArticleListener同步文章数据到es中:{}",message);
        }else {
            log.error("SyncArticleListener同步文章数据到es失败:{}",message);
        }

    }
}
