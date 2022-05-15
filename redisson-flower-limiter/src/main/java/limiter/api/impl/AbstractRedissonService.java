package limiter.api.impl;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
import limiter.configuration.LimiterConfig;
import limiter.configuration.RedissonConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 初始化客户端
 *
 * @author kana
 * @date 2022/5/13 17:09
 */

public class AbstractRedissonService {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRedissonService.class);
    private static final String REDISSON_ADDRESS_TEMPLATE = "redis://{}:{}";
    protected static final Integer THREADS = 16;
    protected static final Integer NETTY_THREADS = 32;
    /**
     * RedissonClient实例
     */
    protected RedissonClient redissonClient;

    public AbstractRedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public AbstractRedissonService(LimiterConfig limiterConfig, RedissonConfig redissonConfig) {
        Preconditions.checkArgument(StrUtil.isNotBlank(limiterConfig.getHost()), "域名不能为空");
        Preconditions.checkArgument(StrUtil.isNotBlank(limiterConfig.getPassword()), "密码不能为空");
        // JsonJackson 序列化方式
        Codec codec = new JsonJacksonCodec();
        Integer threads = THREADS;
        Integer nettyThreads = NETTY_THREADS;
        if (Objects.nonNull(redissonConfig.getCodec())) {
            codec = redissonConfig.getCodec();
        }
        if (Objects.nonNull(redissonConfig.getThread())) {
            threads = redissonConfig.getThread();
        }
        if (Objects.nonNull(redissonConfig.getNettyThreads())) {
            nettyThreads = redissonConfig.getNettyThreads();
        }
        final Config redissonCfg = new Config();
        redissonCfg.setCodec(codec)
                .setUseScriptCache(true)
                .setThreads(threads)
                .setNettyThreads(nettyThreads)
                .useSingleServer()
                .setAddress(StrUtil.format(REDISSON_ADDRESS_TEMPLATE, limiterConfig.getHost(), limiterConfig.getPort()))
                .setPassword(limiterConfig.getPassword())
                .setTimeout(limiterConfig.getTimeout())
                .setConnectionMinimumIdleSize(limiterConfig.getMinIdle());
        this.redissonClient = Redisson.create(redissonCfg);
        logger.info("初始化redissonClient");

    }
}
