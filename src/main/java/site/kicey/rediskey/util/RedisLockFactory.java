package site.kicey.rediskey.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Kicey
 */
@Component
public class RedisLockFactory {
    
    StringRedisTemplate stringRedisTemplate;
    
    private RedisLockFactory (StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public RedisLock getLock(String key, String clientId, long expireTime, long waitTime){
        return new RedisLock(stringRedisTemplate, key, clientId, expireTime, waitTime);
    }
}
