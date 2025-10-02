package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.ArticleVisitStreamMess;

public interface ApArticleService extends IService<ApArticle> {
    /**
     * 加载文章列表
     * @param dto
     * @param type 1 加载更多  2 加载最新
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto, Short type);

    /**
     * 加载文章列表
     * @param dto
     * @param type 1 加载更多  2 加载最新
     * @param fisrtPage true 首页 false 非首页  首页不是说明是首页，而是说当前用户第一次加载该频道数据
     *                  比如java，vue频道，用户首次点击进来就是首页，上滑加载更多，则不是首页
     *                  下拉加载最新也不是首页
     * @return
     */
    public ResponseResult load2(ArticleHomeDto dto, Short type,boolean fisrtPage);
    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    public ResponseResult saveArticle(ArticleDto dto);

    /**
     * 加载文章详情 数据回显
     * @param dto
     * @return
     */
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto);

    /**
     * 更新文章的分值
     * @param articleVisitStreamMess
     */
    public void updateArticleScore(ArticleVisitStreamMess articleVisitStreamMess);

}
