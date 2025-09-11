package com.heima.wemedia.service;

import java.io.IOException;
import java.util.Date;

public interface WmNewsTaskService {
    /**
     * 添加文章到任务中
     * @param id
     * @param publishTime
     */
    //只是把任务添加到数据库和redis中，并且延时执行，poll可以自动拉取zset任务到list。但是扫描任务，需要手动执行。
    //扫描任务 是审核，同时审核完毕后，将文章发布到app中
    public void addNewsToTask(Integer id, Date publishTime);

    /**
     * 消费list任务 审核文章并发布到 app 中
     */
    public void scanNewsByTask() throws IOException;
}
