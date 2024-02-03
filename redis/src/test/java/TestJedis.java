import hxj.tech.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

/**
 * ClassName:TestJedis
 * Package:PACKAGE_NAME
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 12:00 PM
 * @Version 1.0
 */
public class TestJedis {
    private Jedis _jedis;

    @BeforeEach
    void setUp() {
        _jedis = JedisConnectionFactory.getJedisPool();
        // Choose database 0
        _jedis.select(0);
    }

    @Test
    void testString() {
        // set key value
        var result = _jedis.set("hxj:redis:user001", "AlbertZhao");
        System.out.println(result);
        // get key
        System.out.println(_jedis.get("hxj:redis:user001"));
    }

    @Test
    void testHash() {
        // set key value
         _jedis.hset("hxj:redis:user002", "name", "AlbertZhao");
         _jedis.hset("hxj:redis:user002", "age", "28");
         _jedis.hset("hxj:redis:user002", "score", "100");
        // get key
        System.out.println(_jedis.hgetAll("hxj:redis:user002"));
    }

    @AfterEach
    void tearDown() {
        if (_jedis != null){
            _jedis.close();
        }
    }
}
