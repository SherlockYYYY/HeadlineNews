package com.heima.article.service;

public interface HotArticleService {

    /**
     * 计算热点文章，保存到redis中。用于各个频道的推荐
     */
    public void computeHotArticle();
}
