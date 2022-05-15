package limiter.exception;

public class ParamException extends RuntimeException {
    private static final long serialVersionUID = 5524681570122303333L;
    private String param;
    private String msg;

    public ParamException(String code, String msg) {
        this.param = code;
        this.msg = msg;
    }

    public static void throwException(String param, String msg) {
        throw new ParamException(param, msg);
    }

    public String getParam() {
        return this.param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String toString() {
        return "流控注解参数异常,paramKey: " + this.param + " ; msg : " + this.msg;
    }
}
