package limiter.aspect;

import cn.hutool.core.util.StrUtil;
import limiter.annotation.Limiter;
import limiter.api.LimiterService;
import limiter.exception.BlockException;
import limiter.util.MethodUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.RRateLimiter;
import org.redisson.client.RedisResponseTimeoutException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

/**
 * 流控切面
 *
 * @author kana
 * @date 2022/4/20 11:12
 */
@Aspect
@Slf4j
public class LimiterAspect extends AbstractLimiterAspectSupport {

    private LimiterService limiterService;

    public LimiterAspect(LimiterService limiterService, ApplicationContext applicationContext) {
        super(applicationContext);
        this.limiterService = limiterService;
    }

    @Pointcut("@annotation(limiter.annotation.Limiter)")
    public void limiterAnnotationPointcut() {
    }

    @Around("limiterAnnotationPointcut()")
    public Object invokeLimiter(ProceedingJoinPoint pjp) throws Throwable {
        Method originMethod = resolveMethod(pjp);
        Limiter annotation = originMethod.getAnnotation(Limiter.class);
        if (annotation == null) {
            throw new IllegalStateException("注解状态异常");
        }
        String resourceName = getResourceNameWrapperArgs(annotation, originMethod, pjp);
        try {
            limiterService.initRateLimiter(annotation.rate(), annotation.rateInterval(), annotation.rateIntervalUnit(), annotation.rateType(), resourceName);
            RRateLimiter rateLimiter = limiterService.getRateLimiter(resourceName);
            //阻塞式流控
            if (annotation.isBlock()) {
                try {
                    rateLimiter.acquire(annotation.permits());
                } catch (Throwable ex) {
                    //返回超时流控阻塞就降级处理
                    if (ex instanceof RedisResponseTimeoutException) {
                        throw new BlockException("流控阻塞");
                    }
                }
                return pjp.proceed();
            }
            //非阻塞
            if (!rateLimiter.tryAcquire(annotation.permits(), annotation.timeout(), annotation.unit())) {
                throw new BlockException("流控阻塞");
            }
            return pjp.proceed();
        } catch (Throwable ex) {
            if (ex instanceof BlockException) {
                if (StrUtil.isNotBlank(annotation.fallback())) {
                    log.warn(toWarnStringLimiter(annotation, resourceName, MethodUtil.resolveMethodName(originMethod), getFallbackMethodName(pjp, annotation.fallback(), annotation.fallbackClass())));
                    return handleFallback(pjp, annotation, ex);
                }
                return "";
            }
            log.error(toErrorStringLimiter(annotation, resourceName, MethodUtil.resolveMethodName(originMethod), getFallbackMethodName(pjp, annotation.fallback(), annotation.fallbackClass())));
            return pjp.proceed();
        }
    }

}
