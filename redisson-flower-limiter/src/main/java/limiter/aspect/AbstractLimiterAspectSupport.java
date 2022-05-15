package limiter.aspect;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import limiter.annotation.Limiter;
import limiter.exception.ParamException;
import limiter.util.MethodUtil;
import limiter.util.ReflectionsUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RateIntervalUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 抽离一些通用的方法
 *
 * @author kana
 * @date 2022/4/20 11:22
 */

public abstract class AbstractLimiterAspectSupport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLimiterAspectSupport.class);

    private static final Map<String, Method> FALLBACK_MAP = Maps.newConcurrentMap();

    private static final Map<RateIntervalUnit, String> RATE_INTERVAL_UNIT_STRING_MAP = Maps.newHashMap();

    private static final String FLOWER_ERROR_INFO_TEMPLATE = "流控异常,流控的方法:{},流控器名:{},流控量级:每{}{}生成{}个令牌,流控降级执行的方法:{}";

    private static final String FLOWER_WARN_INFO_TEMPLATE = "流控阻塞,流控的方法:{},流控器名:{},流控量级:每{}{}生成{}个令牌,流控降级执行的方法:{}";

    private ApplicationContext applicationContext;

    static {
        RATE_INTERVAL_UNIT_STRING_MAP.put(RateIntervalUnit.MILLISECONDS, "毫秒");
        RATE_INTERVAL_UNIT_STRING_MAP.put(RateIntervalUnit.SECONDS, "秒");
        RATE_INTERVAL_UNIT_STRING_MAP.put(RateIntervalUnit.MINUTES, "分钟");
        RATE_INTERVAL_UNIT_STRING_MAP.put(RateIntervalUnit.HOURS, "小时");
        RATE_INTERVAL_UNIT_STRING_MAP.put(RateIntervalUnit.DAYS, "天");
    }

    public AbstractLimiterAspectSupport(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected Object handleFallback(ProceedingJoinPoint pjp, Limiter annotation, Throwable ex) {
        return handleFallback(pjp, annotation.fallback(), annotation.fallbackClass(), ex);
    }

    protected Object handleFallback(ProceedingJoinPoint pjp, String fallback,
                                    Class<?>[] fallbackClass, Throwable ex) {
        Object[] originArgs = pjp.getArgs();
        Object target = pjp.getTarget();
        // Execute fallback function if configured.
        Method fallbackMethod = extractFallbackMethod(pjp, fallback, fallbackClass);
        if (fallbackMethod != null) {
            target = applicationContext.getBean(fallbackClass[0]);
            // Construct args.
            int paramCount = fallbackMethod.getParameterTypes().length;
            Object[] args;
            if (paramCount == originArgs.length) {
                args = originArgs;
            } else {
                args = Arrays.copyOf(originArgs, originArgs.length + 1);
                args[args.length - 1] = ex;
            }
            return ReflectUtil.invoke(target, fallbackMethod, args);
        }
        return null;
    }

    protected String getFallbackMethodName(ProceedingJoinPoint pjp, String fallback, Class<?>[] fallbackClass) {
        Method fallbackMethod = extractFallbackMethod(pjp, fallback, fallbackClass);
        if (Objects.isNull(fallbackMethod)) {
            return StrUtil.EMPTY;
        }
        return MethodUtil.resolveMethodName(fallbackMethod);
    }


    protected Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        Method method = getDeclaredMethodFor(targetClass, signature.getName(),
                signature.getMethod().getParameterTypes());
        if (method == null) {
            throw new IllegalStateException("Cannot resolve target method: " + signature.getMethod().getName());
        }
        return method;
    }

    protected String getResourceNameWrapperArgs(Limiter limiter, /*@NonNull*/ Method method, ProceedingJoinPoint pjp) {
        Object[] realArgs = pjp.getArgs();
        String resourceName = getResourceName(limiter.value(), method);
        String[] parameters = limiter.args();
        if (parameters != null && parameters.length > 0) {
            StringBuilder builder = new StringBuilder(resourceName);
            for (String parameter : parameters) {
                Object keyValue = getKeyValue(parameter, realArgs);
                builder.append(keyValue);
            }
            resourceName = builder.toString();
        }
        return resourceName;
    }

    protected String getResourceName(String resourceName, /*@NonNull*/ Method method) {
        // If resource name is present in annotation, use this value.
        if (StrUtil.isNotBlank(resourceName)) {
            return resourceName;
        }
        // Parse name of target method.
        return MethodUtil.resolveMethodName(method);
    }

    protected static String toErrorStringLimiter(Limiter limiter, String resourceName, String methodName, String fallbackMethodName) {
        return StrUtil.format(FLOWER_ERROR_INFO_TEMPLATE, methodName, resourceName, limiter.rateInterval(), RATE_INTERVAL_UNIT_STRING_MAP.get(limiter.rateIntervalUnit()), limiter.rate(), fallbackMethodName);
    }

    protected static String toWarnStringLimiter(Limiter limiter, String resourceName, String methodName, String fallbackMethodName) {
        return StrUtil.format(FLOWER_WARN_INFO_TEMPLATE, methodName, resourceName, limiter.rateInterval(), RATE_INTERVAL_UNIT_STRING_MAP.get(limiter.rateIntervalUnit()), limiter.rate(), fallbackMethodName);
    }

    private Method extractFallbackMethod(ProceedingJoinPoint pjp, String fallbackName, Class<?>[] locationClass) {
        if (StrUtil.isBlank(fallbackName)) {
            return null;
        }
        boolean hasLoadingClass = locationClass != null && locationClass.length >= 1;
        Class<?> clazz = hasLoadingClass ? locationClass[0] : pjp.getTarget().getClass();
        String methodKey = getKey(clazz, fallbackName);
        Method m = FALLBACK_MAP.get(methodKey);
        if (m == null) {
            // First time, resolve the fallback.
            Method method = resolveFallbackInternal(pjp, fallbackName, clazz);
            if (method == null) {
                return null;
            }
            // Cache the method instance.
            FALLBACK_MAP.put(getKey(clazz, fallbackName), method);
            return method;
        }
        return m;
    }

    private static String getKey(Class<?> clazz, String name) {
        return String.format("%s:%s", clazz.getCanonicalName(), name);
    }

    private Method resolveFallbackInternal(ProceedingJoinPoint pjp, /*@NonNull*/ String name, Class<?> clazz) {
        Method originMethod = resolveMethod(pjp);
        // Fallback function allows two kinds of parameter list.
        Class<?>[] defaultParamTypes = originMethod.getParameterTypes();
        Class<?>[] paramTypesWithException = Arrays.copyOf(defaultParamTypes, defaultParamTypes.length + 1);
        paramTypesWithException[paramTypesWithException.length - 1] = Throwable.class;
        // We first find the fallback matching the signature of origin method.
        Method method = findMethod(clazz, name, originMethod.getReturnType(), defaultParamTypes);
        // If fallback matching the origin method is absent, we then try to find the other one.
        if (method == null) {
            method = findMethod(clazz, name, originMethod.getReturnType(), paramTypesWithException);
        }
        return method;
    }

    private Method findMethod(Class<?> clazz, String name, Class<?> returnType,
                              Class<?>... parameterTypes) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (name.equals(method.getName()) && returnType.isAssignableFrom(method.getReturnType())
                    && Arrays.equals(parameterTypes, method.getParameterTypes())) {
                logger.info("Resolved method [{}] in class [{}]", name, clazz.getCanonicalName());
                return method;
            }
        }
        // Current class not found, find in the super classes recursively.
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !Object.class.equals(superClass)) {
            return findMethod(superClass, name, returnType, parameterTypes);
        } else {
            logger.warn("Cannot find method [{}] in class [{}] with parameters {}",
                    name, clazz.getCanonicalName(), Arrays.toString(parameterTypes));
            return null;
        }
    }

    /**
     * Get declared method with provided name and parameterTypes in given class and its super classes.
     * All parameters should be valid.
     *
     * @param clazz          class where the method is located
     * @param name           method name
     * @param parameterTypes method parameter type list
     * @return resolved method, null if not found
     */
    private Method getDeclaredMethodFor(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getDeclaredMethodFor(superClass, name, parameterTypes);
            }
        }
        return null;
    }

    /**
     * @param paramKey #p? or #p?.propertyName
     * @param args
     * @return
     */
    private static Object getKeyValue(String paramKey, Object[] args) {
        try {
            Object arg;
            if (paramKey.contains(".")) {
                String position = paramKey.split("\\.")[0];
                int argsIndex = Integer.parseInt(position.replace("#p", ""));
                String propertyName = paramKey.split("\\.")[1];
                arg = args[argsIndex];
                if (arg instanceof List) {
                    arg = ReflectionsUtil.getFieldValue(((List) arg).get(0), propertyName);
                } else if (arg instanceof String[]) {
                    arg = ReflectionsUtil.getFieldValue(((String[]) arg)[0], propertyName);
                } else {
                    arg = ReflectionsUtil.invokeGetter(args[argsIndex], propertyName);
                }
            } else {
                int argsIndex = Integer.parseInt(paramKey.replace("#p", ""));
                arg = args[argsIndex];
            }
            //list 获取第一个元素
            if (arg instanceof List) {
                return ((List) arg).get(0);
            }
            if (arg instanceof String[]) {
                return ((String[]) arg)[0];
            }
            return arg;
        } catch (Exception e) {
            throw new ParamException(paramKey, e.getMessage());
        }
    }

}
