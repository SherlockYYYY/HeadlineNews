package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;


public interface ArticleFreemarkerService {
    /**
     * 生成静态文件并上传到MinIO
     *
     * @param apArticle
     * @param content
     */
    public void buildArticleToMinio(ApArticle apArticle, String content) throws Exception;
}
