package hxj.tech.springbootredis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class SpringbootRedisApplicationTests {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // JSON 工具
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void contextLoads() {
    }

    @Test
    void testRedisStoreObject() {
        var user = new User("hxj",18);
        redisTemplate.opsForValue().set("hxj:redis:user:001",user);
        var result = (User)redisTemplate.opsForValue().get("hxj:redis:user:001");
        System.out.println(result);
    }

    @Test
    void testRedisStoreString() {
        redisTemplate.opsForValue().set("hxj:redis:token","user");
        var result = redisTemplate.opsForValue().get("hxj:redis:token");
        System.out.println(result);
    }

    @Test
    void testRedisStoreObjectByStringRedisTemplate() {
        var user = new User("hxj002",18);

        try {
            stringRedisTemplate.opsForValue().set("hxj:redis:user:002", mapper.writeValueAsString(user));
            var result = stringRedisTemplate.opsForValue().get("hxj:redis:user:002");
            User userDes = mapper.readValue(result,User.class);
            System.out.println(userDes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRedisStoreHash() {
        stringRedisTemplate.opsForHash().put("hxj:hash:user001","name","albert");
        stringRedisTemplate.opsForHash().put("hxj:hash:user001","age","16");
        var result = stringRedisTemplate.opsForHash().entries("hxj:hash:user001");
        System.out.println(result);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class User {
    private String name;
    private Integer age;
}