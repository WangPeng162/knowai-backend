package com.knowai.knowaibackend.common;

public class UserContext {

    /**
     * 存储当前登录用户id
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 保存用户id
     */
    public static void setUserId(Long userId){
        USER_ID.set(userId);
    }

    /**
     *获取当前用户id
     */
    public static Long getUserId(){
        return USER_ID.get();
    }

    /**
     * 删除当前用户信息
     */
    public static void remove(){
        USER_ID.remove();
    }
}
