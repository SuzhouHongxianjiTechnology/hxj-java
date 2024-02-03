package hxj.tech;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * ClassName:JedisConnectionFactory
 * Package:hxj.tech
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 11:57 AM
 * @Version 1.0
 */
public class JedisConnectionFactory {
    private static final JedisPool _jedisPool;

    static {
        var jedisPoolConfig = new JedisPoolConfig();
        // 1.设置最大连接数
        jedisPoolConfig.setMaxTotal(8);
        // 2.设置最大空闲连接
        jedisPoolConfig.setMaxIdle(8);
        // 3.设置最大空闲连接
        jedisPoolConfig.setMinIdle(0);
        // 4.设置最长等待时间 ms
        jedisPoolConfig.setMaxWaitMillis(200);
        _jedisPool = new JedisPool(jedisPoolConfig,"139.196.89.233",6379,2000,"hxj123.");
    }

    public static Jedis getJedisPool(){
        return _jedisPool.getResource();
    }
}
