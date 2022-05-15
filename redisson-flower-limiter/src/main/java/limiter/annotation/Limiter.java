package limiter.annotation;

import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * todo 对注解中回调方法和类的自检
 * 流控注解
 *
 * @author kana
 * @date 2022/4/20 10:45
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Limiter {

    /**
     * @return 流控器名称 默认使用方法签名
     */
    String value() default "";

    /**
     * @return 流控器命名时根据入参拼接(# p1, # p2...)
     */
    String[] args() default {};

    /**
     * @return 产生的令牌数
     */
    long rate();

    /**
     * @return 令牌桶生成令牌的时间间隔 默认为1个时间单位
     */
    long rateInterval() default 1;

    /**
     * @return 令牌桶生成令牌的时间间隔单位 默认秒
     */
    RateIntervalUnit rateIntervalUnit() default RateIntervalUnit.SECONDS;

    /**
     * @return 每次请求获取的令牌数
     */
    long permits() default 1;

    /**
     * @return 单机还是分布式 默认分布式
     */
    RateType rateType() default RateType.OVERALL;

    /**
     * @return 获取不到令牌时是否阻塞请求
     */
    boolean isBlock() default false;

    /**
     * @return 获取超时时间 默认10个时间单位（非阻塞时）
     */
    long timeout() default 10;

    /**
     * @return 获取超时时间单位 默认秒（非阻塞时）
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * @return 获取令牌失败之后执行的回调方法
     */
    String fallback() default "";

    /**
     * @return 回调方法类
     */
    Class<?>[] fallbackClass() default {};

}
