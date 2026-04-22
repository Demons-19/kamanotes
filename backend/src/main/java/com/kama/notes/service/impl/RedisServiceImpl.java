package com.kama.notes.service.impl;

import com.kama.notes.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 保存数据
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 设置过期时间
    public void setWithExpiry(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    // 为已存在的键设置过期时间
    public boolean expire(String key, long timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, TimeUnit.SECONDS));
    }

    // 获取数据
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 批量获取数据
    public java.util.List<Object> multiGet(java.util.List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    // 删除数据
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 判断键是否存在
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 增加计数
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // 获取 Hash 值
    public Object getHashValue(String hashKey, String key) {
        return redisTemplate.opsForHash().get(hashKey, key);
    }

    // 设置 Hash 值
    public void setHashValue(String hashKey, String key, Object value) {
        redisTemplate.opsForHash().put(hashKey, key, value);
    }

    // 向ZSet中添加成员
    public Boolean zAdd(String key, Object member, double score) {
        return redisTemplate.opsForZSet().add(key, member, score);
    }

    // 增加ZSet成员的分数
    public Double zIncrementScore(String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    // 获取ZSet中指定范围的成员（按分数从高到低）
    public java.util.Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    // 从ZSet中移除成员
    public Long zRemove(String key, Object... members) {
        return redisTemplate.opsForZSet().remove(key, members);
    }

    // 获取ZSet中成员的分数
    public Double zScore(String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    // 删除ZSet
    public void deleteZSet(String key) {
        redisTemplate.delete(key);
    }

    // 如果 key 不存在则设置值
    public boolean setIfAbsent(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));
    }

    // 减少计数
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // 根据模式匹配获取所有的键
    public java.util.Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}
