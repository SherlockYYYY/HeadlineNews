package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.sql.Wrapper;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    @Autowired
    private CacheService cacheService;
    /**
     * 添加延时任务
     *
     * @param task
     * @return
     */
    @Override
    public long addTask(Task task) {
        //添加任务到数据库中
        boolean success = addTaskToDB(task);
        //2. 添加任务到redis
        if(success){
            addTaskToCache(task);
            //2.1 如果任务执行时间小于等于当前时间，存入list 立即执行
            //2.2 如果任务执行时间大于当前时间，存入zset 等待执行
        }
        return task.getTaskId();
    }
    /**
     * 添加任务到缓存中
     *
     * @param task
     */
    private void addTaskToCache(Task task) {
        //获取五分钟后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);
        long nextScheduleTime = calendar.getTimeInMillis();
        //2.1 如果任务执行时间小于等于当前时间，存入list 立即执行
        String key = task.getTaskType()+"_"+task.getPriority();
        if(task.getExecuteTime()<=System.currentTimeMillis()){
            cacheService.lLeftPush( ScheduleConstants.TOPIC+key, JSON.toJSONString( task));
        }
        //2.2 如果任务执行时间大于当前时间+5分钟，存入zset 等待执行
        else if(task.getExecuteTime()<=nextScheduleTime){
            cacheService.zAdd(ScheduleConstants.FUTURE+key,JSON.toJSONString( task),task.getExecuteTime());
        }
    }

    /**
     * 添加任务到数据库中
     *
     * @param task
     * @return
     */
    private boolean addTaskToDB(Task task){
        boolean flag = false ;
        try{
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);
            task.setTaskId(taskinfo.getTaskId());
            //保存任务日志表
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;
        }catch (Exception e){
            log.error("添加任务到数据库中失败",e);
        }
        return flag;
    }

    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        boolean flag = false;
        //删除任务表数据 更新日志表数据状态
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);
        if(task!=null){
            //删除redis数据
            removeTaskFromCache(task);
            flag = true;
        }
        //删除redis数据
        return flag;
    }
    /**
     * 删除缓存中的任务
     *
     * @param task
     */
    private void removeTaskFromCache(Task task) {
        //获取任务类型和优先级
        String key = task.getTaskType()+"_"+task.getPriority();
        //删除list中的任务
        if(task.getExecuteTime()<=System.currentTimeMillis()){
            cacheService.lRemove(ScheduleConstants.TOPIC+key,0,JSON.toJSONString(task));
        }
        //删除zset中的任务
        else{
            cacheService.zRemove(ScheduleConstants.FUTURE+key,JSON.toJSONString(task));
        }
    }

    /* 更新数据库
     * @param taskId
     * @param cancelled
     * @return
     */
    private Task updateDb(long taskId, int status) {
        //删除任务
        taskinfoMapper.deleteById(taskId);
        //更新任务日志
        TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
        taskinfoLogs.setStatus(status);
        taskinfoLogsMapper.updateById(taskinfoLogs);
        //返回任务
        Task task = new Task();
        BeanUtils.copyProperties(taskinfoLogs,task);
        task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        return task;
    }

    /**
     * 获取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;
        try {
            //从redis中拉取数据
            String key = type+"_"+priority;
            String taskJson = cacheService.lRightPop(ScheduleConstants.TOPIC+key);
            if(StringUtils.isNotBlank(taskJson)){
                task = JSON.parseObject(taskJson, Task.class);
                //修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        }catch (Exception e){
            log.error("获取任务失败",e);
        }
        return task;
    }

    /**
     * 定时任务  future延时任务定时刷新一边
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refreshTask(){

        String token = cacheService.tryLock("FUTURE_TASK_SYNC",1000*30);
        if(token!=null){
            log.info("未来任务定时刷新开始:"+System.currentTimeMillis());
            //获取所有未来数据集合
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE+"*");
            //按照key和分支查询符合条件的数据
            for (String futureKey : futureKeys) {
                //获取所有未来数据
                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

                String topicKey = ScheduleConstants.TOPIC+futureKey.split(ScheduleConstants.FUTURE)[1];
                //同步数据
                if(!tasks.isEmpty()){
                    cacheService.refreshWithPipeline(futureKey,topicKey,tasks);
                    log.info("成功的将"+futureKey+"中的数据同步到"+topicKey);
                }
            }
        }
    }
    /**
     * 数据库任务定时同步到redis中
     */
    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadTask(){
        //清理缓存中的数据 list  zset
        log.info("数据库任务定时同步到redis中开始");
         clearCache();
        //查询符合条件的任务 小于未来五分钟的数据
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);
        List<Taskinfo> taskinfoList = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery()
                .lt(Taskinfo::getExecuteTime,calendar.getTime()));
        //吧任务同步到redis中
        if(taskinfoList!=null && taskinfoList.size()>0){
            for(Taskinfo taskinfo : taskinfoList){
                //添加到缓存中
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo,task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }

        log.info("数据库任务定时同步到redis中结束");
    }
    /**
     * 清理缓存中的数据
     */
    public void clearCache(){
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);
    }
}
