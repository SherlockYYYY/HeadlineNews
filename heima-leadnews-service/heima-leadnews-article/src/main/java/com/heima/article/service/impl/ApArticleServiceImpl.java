package com.heima.article.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    @Autowired
    private CacheService cacheService;


    private final static short MAX_PAGE_SIZE = 50;
    /**
     * 加载文章列表
     * @param dto
     * @param type 1 加载更多  2 加载最新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //1.校验参数
        // 分页条数校验
        Integer size = dto.getSize();
        if (size == null || size <= 0) {
            size = 10;
        }
        //分餐参数不超过50
        size = Math.max(size,MAX_PAGE_SIZE);
        //校验参数
        if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        // 频道参数校验
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        // 时间校验
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }
        //2.查询
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);
        //3.返回结果
        return ResponseResult.okResult(apArticles);
    }

    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {

        //1.检查参数
        if(dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto,apArticle);
        //2.是否存在id 不存在id 就保存 文章，文章配置，文章内容
        if(dto.getId() == null){
            //2.1 不存在id 就保存 文章，文章配置，文章内容
            save(apArticle);

            //保存配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            //保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        }else {
            //2.2 存在id 修改文章，文章内容  配置不让改
            //修改文章
            updateById(apArticle);
            //修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(new LambdaQueryWrapper<ApArticleContent>().eq(ApArticleContent::getArticleId,dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);

        }

        //异步调用生成静态文件上传到minio

        try {
            articleFreemarkerService.buildArticleToMinio(apArticle, dto.getContent());
        } catch (Exception e) {
            log.error("生成静态文件上传到minio失败",e);
            throw new RuntimeException("生成静态文件上传到minio失败");
        }

        //因为往es中索引(也就是表)添加文档(也就是记录)
        // 有个staticurl字段 所以要等上传minio生成该字段之后才能发送消息让es更新索引

        //3.结果返回
        return ResponseResult.okResult(apArticle.getId());
    }

    /**
     * 加载文章详情 数据回显
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        if(dto == null || dto.getArticleId() == null || dto.getAuthorId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //{ "isfollow": true, "islike": true,"isunlike": false,"iscollection": true }
        boolean isfollow = false, islike = false, isunlike = false, iscollection = false;

        ApUser apUser = AppThreadLocalUtil.getUser();
        if(apUser != null){
            String likeBehaviorJson = ( String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId().toString(), apUser.getId().toString());
            if (StringUtils.isNotBlank(likeBehaviorJson)){
                islike = true;
            }
            //不喜欢的行为
            String unLikeBehaviorJson = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId().toString(), apUser.getId().toString());
            if(StringUtils.isNotBlank(unLikeBehaviorJson)){
                isunlike = true;
            }
            //是否收藏
            String collctionJson = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR+apUser.getId(),dto.getArticleId().toString());
            if(StringUtils.isNotBlank(collctionJson)){
                iscollection = true;
            }

            //是否关注
            Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION + apUser.getId(), dto.getAuthorId().toString());
            System.out.println(score);
            if(score != null){
                isfollow = true;
            }
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("isfollow",isfollow);
        resultMap.put("islike",islike);
        resultMap.put("isunlike",isunlike);
        resultMap.put("iscollection",iscollection);
        return ResponseResult.okResult(resultMap);


    }

    /**
     * 加载文章列表
     *
     * @param dto
     * @param type      1 加载更多  2 加载最新
     * @param fisrtPage true 首页 false 非首页
     * @return
     */
    @Override
    public ResponseResult load2(ArticleHomeDto dto, Short type, boolean fisrtPage) {
        if (fisrtPage){ //首页 直接缓存中拿最热门的文章
            String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if(StringUtils.isNotBlank(jsonStr)){
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                return ResponseResult.okResult(hotArticleVoList);
            }
        }
        return load(dto,type);
    }
}
