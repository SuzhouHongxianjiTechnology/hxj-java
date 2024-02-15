package com.hmdp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryAndOrderTypeList() {
        // 1. 根据缓存查询商户类型
        List<String> shopTypeStringList = stringRedisTemplate.opsForList().range(CACHE_SHOPTYPE_KEY, 0, -1);
        List<ShopType> shopTypeList = new ArrayList<>();

        if(CollUtil.isNotEmpty(shopTypeStringList)){
            shopTypeList = shopTypeStringList.stream().map(type -> JSONUtil.toBean(type, ShopType.class))
                    .collect(Collectors.toList());

            return Result.ok(shopTypeList);
        }

        // 2. 为空的话则查询数据库并插入到缓存中
        shopTypeList = query().orderByAsc("sort").list();

        if(CollUtil.isEmpty(shopTypeList)){
            return Result.fail("不存在商品类型");
        }

        //查到了转为json字符串，存入redis
        for (ShopType shopType : shopTypeList) {
            String jsonStr = JSONUtil.toJsonStr(shopType);
            shopTypeStringList.add(jsonStr);
        }

        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOPTYPE_KEY,shopTypeStringList);

        return Result.ok(shopTypeList);
    }
}
