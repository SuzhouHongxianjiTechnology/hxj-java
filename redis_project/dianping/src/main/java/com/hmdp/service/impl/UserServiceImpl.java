package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 session
        // session.setAttribute("code",code);
        // 4.保存到 Redis 中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码，调用第三方平台
        log.debug("发送短信验证码成功："+code);

        // 返回 ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 2.从 session 中校验验证码
        // Object cacheCode = session.getAttribute("code");

        // 2.从 Redis 中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        String code = loginForm.getCode();
        // 3.不一致直接报错
        if(cacheCode == null||!cacheCode.equals(code)){
            return Result.fail("验证码不一致");
        }
        // 4.一致，根据手机号查询用户 select * from tb_user where phone = ?
        // 会直接使用 mybatis-plus
        User user = query().eq("phone", phone).one();

        // 5.判断用户是否存在
        if(user == null){
            // 6.不存在，创建新用户并保存
            user = createUserWithPhone(phone);

            // 7.保存到数据库
            save(user);
        }

        // 8.保存用户信息到 session 中
        // 这边要做 DTO 转换，存储部分信息
        // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        // 8.保存用户信息到 redis 中（用随机生成的 token 做 key，Hash 结构作为 value）
        // 并将 token 返回给前端
        // 不带中划线的token
        // 8.1 生成随机 token
        String token = UUID.randomUUID().toString(true);
        // 8.2 将 User 对象转换为 UserDTO 并转换为 HashMap 存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        // 8.3 存储到 Redis 中，并设置和 Session 默认有效期 30 分钟一样的有效期
        // 这边有效期有点问题的，通常状态下应该是客户端访问后要刷新有效期
        // 只有不访问才会断开有效期
        String tokenKey = LOGIN_USER_KEY+token;
        // 这边如果直接转换会出现错误，因为 userMap 这个对象有属性不是 String 类型
        // 默认的 StringRedisTemplate 无法转换，可以通过 beanToMap 自定义选项来实现
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.SECONDS);
        // 8.4 返回 token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));

        return user;
    }
}
