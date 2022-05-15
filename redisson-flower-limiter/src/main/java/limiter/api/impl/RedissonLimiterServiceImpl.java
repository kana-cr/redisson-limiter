package limiter.api.impl;

import com.google.common.collect.Maps;
import limiter.api.LimiterService;
import limiter.configuration.LimiterConfig;
import limiter.configuration.RedissonConfig;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redisson限流
 *
 * @author kana
 * @date 2022/4/19 17:49
 */

public class RedissonLimiterServiceImpl extends AbstractRedissonService implements LimiterService, DisposableBean {

    /**
     * 客户端流控器Map
     */
    private final Map<String, RRateLimiter> limiterMap = Maps.newConcurrentMap();

    public RedissonLimiterServiceImpl(RedissonClient redissonClient) {
        super(redissonClient);
    }

    public RedissonLimiterServiceImpl(LimiterConfig limiterConfig, RedissonConfig redissonConfig) {
        super(limiterConfig, redissonConfig);
    }

    @Override
    public void initRateLimiter(long rate, long rateInterval, RateIntervalUnit rateIntervalUnit, String name) {
        this.initRateLimiter(rate, rateInterval, rateIntervalUnit, RateType.OVERALL, name);
    }

    @Override
    public void initRateLimiter(long rate, long rateInterval, RateIntervalUnit rateIntervalUnit, RateType rateType, String name) {
        if (limiterMap.containsKey(name)) {
            return;
        }
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(name);
        //如果初始化失败说明已经存在，可能是之前重启过而key未清理 更新一下key
        if (!rateLimiter.trySetRate(rateType, rate, rateInterval, rateIntervalUnit)) {
            rateLimiter.setRate(rateType, rate, rateInterval, rateIntervalUnit);
            limiterMap.putIfAbsent(name, rateLimiter);
            return;
        }
        limiterMap.putIfAbsent(name, rateLimiter);
    }

    @Override
    public RRateLimiter getRateLimiter(String name) {
        return limiterMap.get(name);
    }

    @Override
    public Boolean tryAcquire(String name) {
        if (!limiterMap.containsKey(name)) {
            return false;
        }
        return limiterMap.get(name).tryAcquire();
    }

    @Override
    public Boolean tryAcquire(String name, long permits) {
        if (!limiterMap.containsKey(name)) {
            return false;
        }
        return limiterMap.get(name).tryAcquire(permits);
    }

    @Override
    public Boolean tryAcquire(String name, long timeout, TimeUnit unit) {
        if (!limiterMap.containsKey(name)) {
            return false;
        }
        return limiterMap.get(name).tryAcquire(timeout, unit);
    }

    @Override
    public Boolean tryAcquire(String name, long permits, long timeout, TimeUnit unit) {
        if (!limiterMap.containsKey(name)) {
            return false;
        }
        return limiterMap.get(name).tryAcquire(permits, timeout, unit);
    }

    @Override
    public void acquire(String name) {
        if (!limiterMap.containsKey(name)) {
            return;
        }
        limiterMap.get(name).acquire();
    }

    @Override
    public void acquire(String name, long permits) {
        if (!limiterMap.containsKey(name)) {
            return;
        }
        limiterMap.get(name).acquire(permits);
    }

    @Override
    public RedissonClient getRedissonClient() {
        return this.redissonClient;
    }

    @Override
    public void destroy() throws Exception {
        // 关闭Redisson实例
        redissonClient.shutdown();
    }

   /* private void initLimitConfig(LimiterConfig limiterConfig, ApplicationContext applicationContext) {
        Map<String, RedisTemplate> redisTemplateMap = applicationContext.getBeansOfType(RedisTemplate.class);
        if (MapUtil.isNotEmpty(redisTemplateMap)) {
            RedisTemplate redisTemplate = null;
            Set<Map.Entry<String, RedisTemplate>> entrySet = redisTemplateMap.entrySet();
            for (Map.Entry<String, RedisTemplate> entry : entrySet) {
                if (Objects.nonNull(redisTemplate)) {
                    break;
                }
                redisTemplate = entry.getValue();
            }
            if (Objects.isNull(redisTemplate)){
                Jedis jedis = applicationContext.getBean(Jedis.class);
                jedis.getDB();
                Client client = jedis.getClient();
            }
        }
    }*/
}
