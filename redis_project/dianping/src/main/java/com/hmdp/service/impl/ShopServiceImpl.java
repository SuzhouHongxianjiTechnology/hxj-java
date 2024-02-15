package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 这里需要声明一个线程池，因为下面我们需要新建一个线程来完成重构缓存
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        // 缓存穿透，两层都不存在值，导致一直将请求打到数据库上
        //Shop shop = queryWithPassThrough(id);
        // 缓存击穿--互斥锁
        //Shop shop = queryWithMutex(id);
        // 缓存击穿--逻辑过期
        Shop shop = queryWithLogicExpiretime(id);

        if (shop == null) {
            return Result.fail("店铺不存在！！");
        }

        return Result.ok(shop);
    }



    // 1. 热点数据缓存击穿--互斥锁解决
    private Shop queryWithMutex(Long id){
        // 1. 查询 Redis 缓存中商品是否存在
        String shopCache = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        Shop shop = null;

        // 2. 判断 shopCache 是否存在，存在则反序列化给前端
        if (StringUtils.isNotBlank(shopCache)) {
            shop = JSONUtil.toBean(shopCache, Shop.class);

            return shop;
        }

        // 如果查询到的是空字符串，则说明是我们缓存的空数据
        if("".equals(shopCache)){
            return null;
        }

        // 3. 不存在则尝试获取锁
        String lockShopKey = LOCK_SHOP_KEY + id;

        try {
            Boolean flag = tryLock(lockShopKey);

            // 如果没有成功加锁，则说明有其他线程获取了锁正在加锁，这边就要进行尝试
            if(!flag){
                // ctrl+alt+t 可以将选中代码块选择包裹
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            // 如果成功加锁了，说明没有其他线程并发加锁，则进行数据库查询，更新缓存。
            shop = getById(id);

            if(shop == null){
                //这里的常量值是 2 分钟，如果从缓存中查询不到则写 ""
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            // 过期时间 30 分钟
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            releaseLock(lockShopKey);
        }

        return shop;
    }

    // 2. 热点数据缓存击穿--逻辑过期时间解决
    private Shop queryWithLogicExpiretime(Long id){
        // 1. 从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2. 如果未命中，则返回空
        if (StringUtils.isBlank(json)) {
            return null;
        }
        // 3. 命中，将 json 反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 3.1 将 data 转为 Shop 对象
        JSONObject shopJson = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(shopJson, Shop.class);
        // 3.2 获取过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4. 判断是否过期
        if (LocalDateTime.now().isBefore(expireTime)) {
            // 5. 未过期，直接返回商铺信息
            return shop;
        }
        // 6. 过期，尝试获取互斥锁
        boolean flag = tryLock(LOCK_SHOP_KEY + id);
        // 7. 获取到了锁
        if (flag) {
            // 8. 开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShop2Redis(id, LOCK_SHOP_TTL);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    this.releaseLock(LOCK_SHOP_KEY + id);
                }
            });
            // 9. 直接返回商铺信息
            return shop;
        }
        // 10. 未获取到锁，直接返回商铺信息
        return shop;
    }

    /// 缓存穿透
    private Shop queryWithPassThrough(Long id){
        // 1. 查询 Redis 缓存中商品是否存在
        String shopCache = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        Shop shop = null;

        // 2. 判断 shopCache 是否存在，存在则反序列化给前端
        if (StringUtils.isNotBlank(shopCache)) {
            shop = JSONUtil.toBean(shopCache, Shop.class);

            return shop;
        }

        // 如果查询到的是空字符串，则说明是我们缓存的空数据
        if("".equals(shopCache)){
            return null;
        }

        // 3. 不存在则查询数据库填入到缓冲中
        shop = getById(id);

        if(shop == null){
            //这里的常量值是 2 分钟，如果从缓存中查询不到则写 ""
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 过期时间 30 分钟
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }

    public void saveShop2Redis(Long id, Long expirSeconds) {
        Shop shop = getById(id);
        RedisData<Shop> redisData = new RedisData<Shop>();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expirSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    // 加锁
    private Boolean tryLock(String lockKey){
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);

        return BooleanUtil.isTrue(result);
    }

    private void releaseLock(String lockKey){
        stringRedisTemplate.delete(lockKey);
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        // 1. 查询 shop.id 是否是空，
        if(shop.getId() == null){
            return Result.fail("商铺 Id 不能为空");
        }
        // 2. 如果不为空，则更新数据库
        updateById(shop);
        // 3. 并删除缓存（如果删除缓存失败，则回滚数据库）
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }
}
