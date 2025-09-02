package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
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
            }
        } catch (Exception e) {
            log.error("生成静态文件失败，文章ID: {}", apArticle.getId(), e);
            throw new RuntimeException("生成静态文件失败", e);
        }
    }
}
