package limiter.configuration;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 流控基础配置
 *
 * @author kana
 * @date 2022/4/19 17:39
 */
@Data
@Accessors(chain = true)
public class LimiterConfig {

    /**
     * redis地址
     */
    private String host;

    /**
     * redis端口
     */
    private Integer port = 6379;

    /**
     * redis db编号
     */
    private Integer database = 0;

    /**
     * redis密码
     */
    private String password;

    /**
     * 默认超时时间 500ms
     */
    private Integer timeout = 500;

    /**
     * 最小连接数 默认0
     */
    private Integer minIdle = 0;

}
