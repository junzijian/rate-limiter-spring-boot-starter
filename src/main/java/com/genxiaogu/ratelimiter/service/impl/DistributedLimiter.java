package com.genxiaogu.ratelimiter.service.impl;

import com.genxiaogu.ratelimiter.common.JedisUtil;
import com.genxiaogu.ratelimiter.service.Limiter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.UUID;

import static com.genxiaogu.ratelimiter.dto.LimitDTO.*;
import static com.genxiaogu.ratelimiter.lock.LuaScriptLock.*;

/**
 * Created by junzijian on 2017/7/4.
 */
public class DistributedLimiter implements Limiter {

    private static final Logger logger = LogManager.getLogger(DistributedLimiter.class);

    public static final long LOCK_TIME_OUT = 1000;

    public static final long KEY_TIME_OUT = 1000;

    private RedisTemplate redisTemplate;

    private String route;

    private Integer limit;

    /**
     * @param redisTemplate
     */
    public DistributedLimiter(RedisTemplate redisTemplate, String route, Integer limit) {
        this.redisTemplate = redisTemplate;
        this.route = route;
        this.limit = limit;
    }

    public DistributedLimiter(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 第一次如果key存在则从redis删除，并加入到keySet,相当于是初始化
     *
     * @return
     */
    @Override
    public boolean execute() {
        return execute(route, limit, "");
    }

    @Override
    public boolean execute(String route, Integer limit) {
        return execute(route, limit, "");
    }

    /**
     * 限流实现
     *
     * @param route
     * @param limit
     * @param obj
     * @return
     */
    @Override
    public boolean execute(String route, Integer limit, String obj) {

        final String key = route.concat(obj);
        final String lockKey = "lock:" + key;
        final String lockValue = UUID.randomUUID().toString();

        boolean bool = false;
        try {
            if (getLock(redisTemplate, new ArrayList<String>() {{
                add(lockKey);
            }}, lockValue, String.valueOf(LOCK_TIME_OUT))) {

                // doSomething
                bool = execLimit(redisTemplate, key, String.valueOf(limit), String.valueOf(KEY_TIME_OUT));
            }
        } catch (Exception e) {
            logger.error("DistributedLimiter execute error.", e);
        } finally {
            releaseLock(redisTemplate, new ArrayList<String>() {{
                add(lockKey);
            }}, lockValue);
        }

        return bool;
    }


    // ------------------------------------以下代码已废弃！仅留作笔记用----------------------------------------------------
    // ------------------------------------以下代码已废弃！仅留作笔记用----------------------------------------------------
    // ------------------------------------以下代码已废弃！仅留作笔记用----------------------------------------------------


    /**
     * 3
     * <p>
     * 留作笔记用
     *
     * @param route
     * @param limit
     * @param obj
     * @return
     */
    public boolean execute3(String route, Integer limit, String obj) {

        final byte[] key = route.concat(obj).getBytes();
        final byte[] lockKey = ("lock:" + route.concat(obj)).getBytes();
        final byte[] lockValue = UUID.randomUUID().toString().getBytes();
        final byte[] lockTimeOut = String.valueOf(LOCK_TIME_OUT).getBytes();
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();

        boolean bool = false;
        try {

            if (getLock3(connection, lockKey, lockValue, lockTimeOut)) {

                // doSomething
                bool = execLimit3(connection, key, limit, KEY_TIME_OUT);
            }

        } catch (Exception e) {
            logger.error("DistributedLimiter execute error.", e);
        } finally {
            releaseLock3(connection, lockKey, lockValue);
        }

        return bool;
    }


    /**
     * 5
     * <p>
     * 留作笔记用
     *
     * 这里确实不需要用到锁
     *
     * @param route
     * @param limit
     * @param obj
     * @return
     */
    public boolean execute5(String route, final Integer limit, String obj) {

        final String key = route.concat(obj);
        final String value = "1";

//        Jedis jedis = JedisUtil.getJedis();
        Jedis jedis = new Jedis("192.168.189.128", 6379);

        String setNxPx = jedis.set(key, value, "NX", "PX", 1000L);

        if (null != setNxPx && setNxPx.equals("OK")) {

            return true;
        } else {

            if (jedis.incr(key) > limit) {
                return false;
            }
            return true;
        }

    }

    // ------------------------------------以上代码已废弃！仅留作笔记用----------------------------------------------------
    // ------------------------------------以上代码已废弃！仅留作笔记用----------------------------------------------------
    // ------------------------------------以上代码已废弃！仅留作笔记用----------------------------------------------------


}
