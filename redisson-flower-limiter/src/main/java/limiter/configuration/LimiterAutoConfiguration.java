package limiter.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 自动装配
 *
 * @author kana
 * @date 2022/4/22 16:33
 */
@Configuration
@Import(LimiterConfigure.class)
@ConditionalOnProperty(prefix = "limiter", value = "enable", havingValue = "true")
public class LimiterAutoConfiguration {
}
