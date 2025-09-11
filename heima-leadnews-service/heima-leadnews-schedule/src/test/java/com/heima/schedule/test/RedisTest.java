package com.heima.schedule.test;

import com.heima.common.redis.CacheService;
import com.heima.schedule.ScheduleApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {
    @Autowired
    private CacheService cacheService;

    @Test
    public void testList() throws Exception {
        //再list左边添加元素
//        cacheService.lLeftPush( "list:test", "1");
        //再list右边获取元素 并删除
        String value = cacheService.lRightPop( "list:test");
        System.out.println(value);
    }
    @Test
    public void testZSet() throws Exception {
        //再zset左边添加元素
        cacheService.zAdd("zset:test", "1", 1);
        cacheService.zAdd("zset:test", "2", 2);
        cacheService.zAdd("zset:test", "3", 3);
        cacheService.zAdd("test", "4", 4);
        //再zset右边获取元素 并删除

        Set<String> set = cacheService.zRangeByScore("zset:test", 0, 3);
        System.out.println(set);
    }
}
