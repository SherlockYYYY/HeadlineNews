package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Cache;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private IWemediaClient wemediaClient;

    @Autowired
    private CacheService cacheService;
    /**
     * 计算热点文章，保存到redis中。用于各个频道的推荐
     */
    @Override
    public void computeHotArticle() {
        //1.查询前五天的文章数据
        Date date = DateTime.now().minusYears(5).toDate();
        List<ApArticle> articleList = apArticleMapper.findArticleListByLast5Days(date);
        //2.计算文章分值
        List<HotArticleVo> hotArticleVoList = computeHotArticle(articleList);
        //3.为每个频道缓存30条分值较高的文章
        cacheTagToRedis(hotArticleVoList);   //tag就是频道
    }

    /**
     * 为每个频道缓存30条分值较高的文章
     *
     * @param hotArticleVoList
     */
    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        //为每个频道缓存30条分值较高的文章
        ResponseResult channels = wemediaClient.getChannels();
        if (channels.getCode().equals(200)){
            //channels是对象 先转成json字符串
            String channelsJson = JSON.toJSONString(channels.getData());
            List<WmChannel> wmChannels = JSON.parseArray(channelsJson, WmChannel.class);
            //检索初每个频道的文章
            if(wmChannels != null && wmChannels.size() > 0){
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    if(hotArticleVos == null || hotArticleVos.size() == 0){
                        continue;
                    }
                    //给文章排序，取30条存入redis  key:频道id value:30条分值较高的文章
                    sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }

        sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);

    }

    /**
     * 给频道排序，取30条数据，存入redis
     *
     * @param
     * @param key
     */
    private void sortAndCache(List<HotArticleVo> hotArticleVoList, String key) {
        //设置推荐数据
        hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        if (hotArticleVoList.size() > 30) {
            hotArticleVoList = hotArticleVoList.subList(0, 30);
        }
        cacheService.set(key, JSON.toJSONString(hotArticleVoList));
    }

    /**
     * 计算文章分值
     *
     * @param articleList
     * @return
     */
    private List<HotArticleVo> computeHotArticle(List<ApArticle> articleList) {

        List<HotArticleVo> hotArticleVoList = new ArrayList<>();
        if (articleList != null && articleList.size() > 0){
            for (ApArticle article : articleList) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(article,hot);
                Integer score = computeScore(article);
                hot.setScore(  score);
                hotArticleVoList.add(hot);
            }
        }

        return hotArticleVoList;
    }

    /**
     * 计算文章分值
     *
     * @param article
     * @return
     */
    private Integer computeScore(ApArticle article) {
        Integer score = 0;
        if (article.getLikes() != null){
            score += article.getLikes() *  ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }

        if (article.getViews() != null){
            score += article.getViews();
        }

        if (article.getComment() != null){
            score += article.getComment() *  ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }

        if (article.getCollection() != null){
            score += article.getCollection() *  ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }

        return score;
    }
}
