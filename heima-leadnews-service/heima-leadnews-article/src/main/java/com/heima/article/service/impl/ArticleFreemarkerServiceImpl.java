package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {
    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 生成静态文件并上传到MinIO
     *
     * @param apArticle
     * @param content
     */
    @Async
    @Override
    public void buildArticleToMinio(ApArticle apArticle, String content) {
        try {
            //1.获取文章内容
            if (StringUtils.isNotBlank(content)) {
                //2.文章内容通过freemarker生成html文件
                Template template = configuration.getTemplate("article.ftl");
                StringWriter out = new StringWriter();

                Map<String, Object> params = new HashMap<>();
                params.put("content", JSONArray.parseArray(content));

                template.process(params, out);
                log.debug("Freemarker模板处理完成，文章ID: {}", apArticle.getId());
                InputStream is = new ByteArrayInputStream(out.toString().getBytes());
                //3.把html文件上传到minio中
                String path = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", is);

                //4.修改ap_article表，保存static_url字段
                ApArticle article = new ApArticle();
                article.setId(apArticle.getId());
                article.setStaticUrl(path);
                apArticleMapper.updateById(article);

                //因为往es中索引(也就是表)添加文档(也就是记录)
                // 有个staticurl字段 所以要等上传minio生成该字段之后才能发送消息让es更新索引

                //发送消息 创建索引
                SearchArticleVo vo = new SearchArticleVo(); //这个vo是给es看的，让es创建索引中的字段
                BeanUtils.copyProperties(apArticle, vo);
                vo.setContent(content);
                vo.setStaticUrl(path);

                kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSONArray.toJSONString(vo));
            }
        } catch (Exception e) {
            log.error("生成静态文件失败，文章ID: {}", apArticle.getId(), e);
            throw new RuntimeException("生成静态文件失败", e);
        }
    }

//    private void createArticleESIndex(ApArticle apArticle, String content, String path) throws InvocationTargetException, IllegalAccessException {
//        SearchArticleVo vo = new SearchArticleVo(); //这个vo是给es看的，让es创建索引中的字段
//        BeanUtils.copyProperties(apArticle, vo);
//        vo.setContent(content);
//        vo.setStaticUrl(path);
//
//        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSONArray.toJSONString(vo));
//    }
}
