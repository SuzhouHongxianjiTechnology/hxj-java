package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_TOKEN_REDIS_CACHE_HEADER;

@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    // 这边不能这样注入，只有 SpringComponent 中可以通过这样的注解实现
    // @Resource
    // private StringRedisTemplate stringRedisTemplate;

    private  StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        // 本类必须加 this
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1.获取 session
//        HttpSession session = request.getSession();
//        // 2.获取 session 中的用户
//        Object user = session.getAttribute("user");
        // 1. 获取请求头中的 Token（这边会在前端调用 login 接口时把 token 返回给前端
        // 前端每次请求都会带着这个 Header 来请求的。
        String token = request.getHeader(USER_TOKEN_REDIS_CACHE_HEADER);

        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 2. 基于 token 获取 redis 中的用户
        String key = LOGIN_USER_KEY+token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3.判断用户是否存在
        if(userMap.isEmpty()){
            return true;
        }

        // 5.1 将从 Redis 中查询到的 Hash 数据转换为 UserDTO 对象。
        UserDTO userDto = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 5.存在则保存用户信息到 ThreadLocal 中
        UserHolder.saveUser(userDto);

        // 6.1 刷新 token 有效期
        stringRedisTemplate.expire(key,LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 6.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户，防止用户信息泄露
        // 因为 ThreadLocal 底层是 ThreadLocalMap，当前线程 ThreadLocal 作为 key（弱引用)
        // UserDTO 作为强引用无法被 jvm 会回收，所以这边必须手动释放。
        UserHolder.removeUser();
    }
}
