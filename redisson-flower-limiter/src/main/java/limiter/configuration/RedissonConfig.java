package limiter.configuration;

import lombok.Data;
import org.redisson.client.codec.Codec;

/**
 * redisson配置
 *
 * @author kana
 * @date 2022/4/21 10:35
 */
@Data
public class RedissonConfig {

    private Integer thread;

    private Integer nettyThreads;

    private Codec codec;
}
