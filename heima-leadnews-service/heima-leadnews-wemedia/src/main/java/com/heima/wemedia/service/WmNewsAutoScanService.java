package com.heima.wemedia.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmNews;

import java.io.IOException;

public interface WmNewsAutoScanService {
    /**
     * 自媒体文章审核  但是没有集成阿里云接口 这里直接审核通过
     * @param id 自媒体文章id
     * @return
     */
    void autoScan(Integer id) throws IOException;

    /**
     * 保存app文章数据
     * @param wmNews
     * @return
     */
    public ResponseResult saveAppArticle(WmNews wmNews);
}
