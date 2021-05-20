package sentinel.customizetion.api.call.common;

import sentinel.customizetion.api.call.common.domian.HttpParam;
import sentinel.customizetion.api.call.common.domian.HttpResult;

/**
 * @author : jiez
 * @date : 2021/2/26 11:08
 */
public interface ApiCallFailureCallback {

    /**
     * 失败处理
     *
     * @param httpParam 参数
     * @param httpResult 返回值
     * @param exception 异常值
     */
    HttpResult callBack(HttpParam httpParam, HttpResult httpResult, Exception exception);
}
