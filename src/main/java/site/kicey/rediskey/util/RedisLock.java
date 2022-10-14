package site.kicey.rediskey.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Kicey
 */
public class RedisLock {
    public final StringRedisTemplate stringRedisTemplate;
    public final String key;
    
    public final long expireTime;
    
    public final String clientId;
    
    public final String keyId;
    
    public final String keyList;
    
    public final long waitTime;
    public RedisLock(StringRedisTemplate stringRedisTemplate, String key, String clientId, long expireTime, long waitTime){
        this.stringRedisTemplate = stringRedisTemplate;
        this.key = key;
        this.expireTime = expireTime;
        this.clientId = clientId;
        keyId = clientId + new Random().nextInt();
        this.waitTime = waitTime;
        keyList = key + "_list";
    }
    
    // redis 的锁是非阻塞的，如果获取不到锁，直接返回 false
    public synchronized boolean trylock() {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, keyId, expireTime, TimeUnit.MILLISECONDS);
        if (Boolean.TRUE.equals(result)) {
            // 如果队列中存在锁，则删除
            stringRedisTemplate.opsForList().leftPop(keyList);
            return true;
        }
        return false;
    }
    
    /**
     * 自旋 一定次数尝试获取锁，如果获取不到锁，直接返回false
     * @param spinCount 自旋次数
     * @return 是否获取到锁
     */
    public boolean spinlock(int spinCount) {
        for (; spinCount > 0; spinCount--) {
            if (trylock()) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized void unlock() {
        String value = stringRedisTemplate.opsForValue().get(key);
        if(!Objects.equals(value, keyId)) {
            System.out.println("unlock failed in thread:" + Thread.currentThread().getName());
            throw new IllegalMonitorStateException();
        }
        stringRedisTemplate.opsForValue().getAndDelete(key);
        stringRedisTemplate.opsForList().rightPush(keyList, keyId);
    }
    
    public void blockLock() throws InterruptedException {
        if(trylock()){
           return; 
        }
        String listValue = stringRedisTemplate.opsForList().leftPop(keyList, waitTime, TimeUnit.MILLISECONDS);
        trylock();
        if(listValue == null){
            throw new InterruptedException();
        }
    }
}
