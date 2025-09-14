package com.heima.utils.thread;

import com.heima.model.user.pojos.ApUser;

public class AppThreadLocalUtil {
    private static final ThreadLocal<ApUser> AP_USER_THREAD_LOCAL = new ThreadLocal<>();

    //将用户id存入线程中
    public static void setUser(ApUser apUser){
        AP_USER_THREAD_LOCAL.set(apUser);
    }

    //获取用户id
    public static ApUser getUser(){
        return AP_USER_THREAD_LOCAL.get();
    }

    //移除用户id
    public static void clear(){ //因为threadlocalkey是弱引用 但是value是强引用 所以线程结束的时候value不会被回收
        AP_USER_THREAD_LOCAL.remove();
    }
}
