package limiter.configuration;

import limiter.api.LimiterService;
import limiter.api.impl.RedissonLimiterServiceImpl;
import limiter.aspect.LimiterAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author kana
 * @date 2022/4/19 17:38
 */

@Configuration
public class LimiterConfigure {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    @ConfigurationProperties("spring.redis")
    @ConditionalOnMissingBean
    public LimiterConfig limiterConfig() {
        return new LimiterConfig();
    }

    @Bean
    @ConfigurationProperties("mid.limiter")
    @ConditionalOnMissingBean
    public RedissonConfig redissonConfig() {
        return new RedissonConfig();
    }


    @Bean
    public LimiterService limiterService() {
        LimiterConfig limiterConfig = this.limiterConfig();
        RedissonConfig redissonConfig = this.redissonConfig();
        return new RedissonLimiterServiceImpl(limiterConfig, redissonConfig);
    }

    @Bean
    public LimiterAspect limiterAspect() {
        return new LimiterAspect(this.limiterService(), applicationContext);
    }

}
