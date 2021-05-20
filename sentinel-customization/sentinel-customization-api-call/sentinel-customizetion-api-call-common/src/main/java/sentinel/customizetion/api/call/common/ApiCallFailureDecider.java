package sentinel.customizetion.api.call.common;


import sentinel.customizetion.api.call.common.domian.HttpParam;
import sentinel.customizetion.api.call.common.domian.HttpResult;

/**
 * @author : jiez
 * @date : 2021/2/26 11:11
 */
public interface ApiCallFailureDecider {

    /**
     * 判定是否失败
     *
     * @param httpParam 参数
     * @param httpResult 返回值
     * @param exception 调用异常信息
     * @return 是否有异常
     */
    boolean decideFault(HttpParam httpParam, HttpResult httpResult, Exception exception);
}
