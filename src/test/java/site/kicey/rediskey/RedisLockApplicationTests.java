package site.kicey.rediskey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import site.kicey.rediskey.util.RedisLock;
import site.kicey.rediskey.util.RedisLockFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class RedisLockApplicationTests {
    
    RedisLock redisLock;
    
    @Test
    void onlyRedis(@Autowired StringRedisTemplate stringRedisTemplate){
        stringRedisTemplate.opsForValue().set("onlyRedis", "ok");
        System.out.println(stringRedisTemplate.opsForValue().getAndDelete("onlyRedis"));
    }
    
    /**
     * 似乎在 ReidsTemplate 执行期间会响应 thread.interrupt() 信号
     * @param stringRedisTemplate redis 模板
     * @throws InterruptedException 中断异常
     */
    @Test
    void multiThreadStringRedisTemplate(@Autowired StringRedisTemplate stringRedisTemplate)
            throws InterruptedException {
        Runnable runnable = () -> {
            for(int i = 0;i < 50;++i){
                System.out.println(Thread.currentThread().isDaemon());
                stringRedisTemplate.opsForValue().set("ok", Integer.toString(i));
                String value = stringRedisTemplate.opsForValue().get("ok");
                System.out.println(i + ":" + value);
                System.out.println("a cycle end");
            }
        };
        // multi thread not work
        ExecutorService executorService = Executors.newCachedThreadPool();
        for(int i = 0;i < 8;++i){
            executorService.execute(runnable);
        }
        Thread.sleep(5000);
        System.out.println("end");
    }
    
    @Test
    void redisKey(@Autowired RedisLockFactory redisLockFactory) throws InterruptedException {
        String clientId = ManagementFactory.getRuntimeMXBean().getName();
        redisLock = redisLockFactory.getLock("test", clientId, 7000, 40000);
    
        Runnable runnable = () -> {
            try {
                redisLock.blockLock();
            } catch (InterruptedException e) {
                System.out.println("Obtain lock timed out, thread:" + Thread.currentThread().getName());
                return;
            }
        
            for (int i = 0; i < 5; ++i) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("cycle: " + i + ", in thread: " + Thread.currentThread().getName());
            }
        
            redisLock.unlock();
        };
    
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 8; ++i) {
            executorService.execute(runnable);
        }
        Thread.sleep(30000);
    }
}
