package com.heima.wemedia.service;

public interface WmNewsAutoScanService {
    /**
     * 自媒体文章审核  但是没有集成阿里云接口 这里直接审核通过
     * @param id 自媒体文章id
     * @return
     */
    void autoScan(Integer id);
}
