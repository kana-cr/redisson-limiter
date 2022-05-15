package limiter.api;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 *
 * @author kana
 * @date 2022/4/19 17:49
 */

public interface LimiterService {

    /**
     * 生成限流器 n分钟内产生m个令牌 默认多客户端共享
     *
     * @param rate             令牌数
     * @param rateInterval     产生令牌间隔时间
     * @param rateIntervalUnit 时间单位
     * @param name             限流器名称 单实例唯一
     */
    void initRateLimiter(long rate, long rateInterval, RateIntervalUnit rateIntervalUnit, String name);

    /**
     * 生成限流器 n分钟内产生m个令牌
     *
     * @param rate             令牌数
     * @param rateInterval     产生令牌间隔时间
     * @param rateIntervalUnit 时间单位
     * @param rateType         是否客户端之间共享
     * @param name             限流器名称 单实例唯一
     */
    void initRateLimiter(long rate, long rateInterval, RateIntervalUnit rateIntervalUnit, RateType rateType, String name);

    /**
     * 获取限流器
     *
     * @param name 限流器名称
     * @return 限流器
     */
    RRateLimiter getRateLimiter(String name);

    /**
     * 尝试获取限流器令牌 不阻塞等待
     *
     * @param name 限流器名称
     * @return 是否获取成功
     */
    Boolean tryAcquire(String name);

    /**
     * 尝试获取多个限流器令牌
     *
     * @param name    限流器名称
     * @param permits 获取令牌数
     * @return 是否获取成功
     */
    Boolean tryAcquire(String name, long permits);

    /**
     * 尝试在一段时间内获取限流器令牌
     *
     * @param name    限流器名称
     * @param timeout 获取时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    Boolean tryAcquire(String name, long timeout, TimeUnit unit);

    /**
     * 尝试在一段时间内获取多个限流器令牌
     *
     * @param name    限流器名称
     * @param permits 获取令牌数
     * @param timeout 获取时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    Boolean tryAcquire(String name, long permits, long timeout, TimeUnit unit);

    /**
     * 尝试获取一个限流器令牌 阻塞等待
     *
     * @param name 限流器名称
     */
    void acquire(String name);

    /**
     * 尝试获取多个限流器令牌
     *
     * @param name    限流器名称
     * @param permits 获取令牌数
     */
    void acquire(String name, long permits);

    /**
     * 获取RedissonClient实例
     *
     * @return 客户端实例
     */
    RedissonClient getRedissonClient();

}
