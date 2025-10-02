package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ArticleIncrHandlerListener {

    @Autowired
    private ApArticleService apArticleService;
    @KafkaListener(topics = HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC)
    public void onMessage(String message){
        if(StringUtils.isNotBlank(message)){
            ArticleVisitStreamMess articleVisitStreamMess = JSON.parseObject(message, ArticleVisitStreamMess.class);
            apArticleService.updateArticleScore(articleVisitStreamMess);
        }
    }
}
