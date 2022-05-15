package limiter.exception;

/**
 * 流控异常
 *
 * @author kana
 * @date 2022/4/20 13:51
 */

public class BlockException extends RuntimeException {
    private static final long serialVersionUID = -3489448858903756426L;

    public BlockException(String message) {
        super(message);
    }
}
