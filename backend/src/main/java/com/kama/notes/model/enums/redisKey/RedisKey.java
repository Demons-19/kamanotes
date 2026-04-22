package com.kama.notes.model.enums.redisKey;

/**
 * Redis 键名管理类
 * 用于统一管理和生成 Redis 中使用的各种键名
 * 遵循 Redis 键名命名规范 : 使用冒号分隔的层级结构
 */
public class RedisKey {
    /**
     * 生成注册验证码的 Redis 键名
     * 
     * @param email 用户邮箱地址
     * @return 格式为 "verification_code:register:{email}" 的 Redis 键名
     */
    public static String registerVerificationCode(String email) {
        return "email:register_verification_code:" + email;
    }

    /**
     * 生成注册验证码限制的 Redis 键名 <br/>
     * 用于记录用户发送验证码的频率限制
     * 
     * @param email 用户邮箱地址
     * @return 格式为 "email:register_verification_code:limit:{email}" 的 Redis 键名
     */
    public static String registerVerificationLimitCode(String email) {
        return "email:register_verification_code:limit:" + email;
    }

    /**
     * 生成邮件任务队列的 Redis 键名
     * 
     * @return 格式为 "queue:email:task" 的 Redis 键名
     */
    public static String emailTaskQueue() {
        return "queue:email:task";
    }

    public static String jwtBlacklist(String token) {
        return "jwt:blacklist:" + token;
    }

    /**
     * 笔记热度排行榜 Redis 键名
     *
     * @return 格式为 "note:hot:rank" 的 Redis 键名
     */
    public static String noteHotRank() {
        return "note:hot:rank";
    }

    /**
     * 笔记热榜结果列表 Redis 键名
     *
     * @param limit 热榜数量限制
     * @return 格式为 "note:hot:rank:list:{limit}" 的 Redis 键名
     */
    public static String noteHotRankList(Integer limit) {
        return "note:hot:rank:list:" + limit;
    }

    /**
     * 笔记详情用户点赞状态 Redis 键名
     */
    public static String noteUserLiked(Long userId, Integer noteId) {
        return "note:user:liked:" + userId + ":" + noteId;
    }

    /**
     * 笔记详情用户收藏状态 Redis 键名
     */
    public static String noteUserCollected(Long userId, Integer noteId) {
        return "note:user:collected:" + userId + ":" + noteId;
    }

    /**
     * 笔记热榜结果列表 Redis 键名匹配模式
     *
     * @return 格式为 "note:hot:rank:list:*" 的匹配模式
     */
    public static String noteHotRankListPattern() {
        return "note:hot:rank:list:*";
    }

    /**
     * 笔记详情 Redis 键名
     *
     * @param noteId 笔记ID
     * @return 格式为 "note:detail:{noteId}" 的 Redis 键名
     */
    public static String noteDetail(Integer noteId) {
        return "note:detail:" + noteId;
    }

    /**
     * 用户登录态基础信息 Redis 键名
     */
    public static String userSessionProfile(Long userId) {
        return "user:session:profile:" + userId;
    }


    public static String noteReviewGroup() {
        return "group:note:review";
    }

    public static String noteReviewRetryZSet() {
        return "zset:note:review:retry";
    }
}
