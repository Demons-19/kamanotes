package com.kama.notes.service;

/**
 * Redis服务接口，定义了对Redis数据库的基本操作
 */
public interface RedisService {
    /**
     * 保存数据到Redis
     *
     * @param key 数据的键
     * @param value 数据的值
     */
    void set(String key, Object value);

    /**
     * 保存数据到Redis并设置过期时间
     *
     * @param key 数据的键
     * @param value 数据的值
     * @param timeout 数据的过期时间，单位秒
     */
    void setWithExpiry(String key, Object value, long timeout);

    /**
     * 为已存在的键设置过期时间
     *
     * @param key 数据的键
     * @param timeout 过期时间，单位秒
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout);

    /**
     * 从Redis获取数据
     *
     * @param key 数据的键
     * @return 数据的值，如果键不存在则返回null
     */
    Object get(String key);

    /**
     * 批量从Redis获取数据
     *
     * @param keys 键列表
     * @return 值的列表，顺序与keys一致，不存在的键对应位置为null
     */
    java.util.List<Object> multiGet(java.util.List<String> keys);

    /**
     * 从Redis删除数据
     *
     * @param key 数据的键
     */
    void delete(String key);

    /**
     * 判断Redis中是否存在指定的键
     *
     * @param key 数据的键
     * @return 如果键存在返回true，否则返回false
     */
    boolean exists(String key);

    /**
     * 增加计数
     *
     * @param key 数据的键
     * @param delta 增加的数值
     * @return 增加后的数值
     */
    Long increment(String key, long delta);

    /**
     * 获取Hash类型数据的值
     *
     * @param hashKey Hash的键
     * @param key 数据的键
     * @return 数据的值，如果键不存在则返回null
     */
    Object getHashValue(String hashKey, String key);

    /**
     * 设置Hash类型数据的值
     *
     * @param hashKey Hash的键
     * @param key 数据的键
     * @param value 数据的值
     */
    void setHashValue(String hashKey, String key, Object value);

    /**
     * 向ZSet中添加成员
     *
     * @param key ZSet的键
     * @param member 成员
     * @param score 分数
     * @return 是否添加成功
     */
    Boolean zAdd(String key, Object member, double score);

    /**
     * 增加ZSet成员的分数
     *
     * @param key ZSet的键
     * @param member 成员
     * @param delta 增量
     * @return 增加后的分数
     */
    Double zIncrementScore(String key, Object member, double delta);

    /**
     * 获取ZSet中指定范围的成员（按分数从高到低）
     *
     * @param key ZSet的键
     * @param start 开始位置
     * @param end 结束位置
     * @return 成员和分数的集合
     */
    java.util.Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end);

    /**
     * 从ZSet中移除成员
     *
     * @param key ZSet的键
     * @param members 要移除的成员
     * @return 移除的成员数量
     */
    Long zRemove(String key, Object... members);

    /**
     * 获取ZSet中成员的分数
     *
     * @param key ZSet的键
     * @param member 成员
     * @return 分数
     */
    Double zScore(String key, Object member);

    /**
     * 删除ZSet
     *
     * @param key ZSet的键
     */
    void deleteZSet(String key);

    /**
     * 如果 key 不存在则设置值
     *
     * @param key 数据的键
     * @param value 数据的值
     * @return 是否设置成功
     */
    boolean setIfAbsent(String key, Object value);

    /**
     * 减少计数
     *
     * @param key 数据的键
     * @param delta 减少的数值
     * @return 减少后的数值
     */
    Long decrement(String key, long delta);

    /**
     * 根据模式匹配获取所有的键
     *
     * @param pattern 匹配模式
     * @return 键的集合
     */
    java.util.Set<String> keys(String pattern);
}
