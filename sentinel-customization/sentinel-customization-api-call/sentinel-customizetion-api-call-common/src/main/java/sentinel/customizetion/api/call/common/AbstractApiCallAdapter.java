package sentinel.customizetion.api.call.common;


import sentinel.customizetion.api.call.common.domian.ApiCallResultCode;
import sentinel.customizetion.api.call.common.domian.HttpParam;
import sentinel.customizetion.api.call.common.domian.HttpResult;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;

import java.util.Objects;

/**
 * @author : jiez
 * @date : 2021/2/25 9:46
 */
public abstract class AbstractApiCallAdapter {

    protected static final ApiCallFailureCallback DEFAULT_API_CALL_FAILURE_CALL_BACK = (httpParam, httpResult, exception) -> {
        // handle sentinel.flowException
        if (exception instanceof FlowException) {
            return HttpResult.limitError(ApiCallResultCode.ApiCallLimitCode.FLOW_LIMIT_ERROR);

        }
        // handle sentinel.blockException
        else if (exception instanceof BlockException) {
            return HttpResult.limitError(ApiCallResultCode.ApiCallLimitCode.OUTER_API_LIMIT_ERROR);

        }
        // handle api error
        if (Objects.nonNull(httpResult)) {
            return httpResult;
        }

        throw new RuntimeException("api调用失败, URL:" + httpParam.getRequestUrl(), exception);
    };

    protected static final ApiCallFailureDecider DEFAULT_API_CALL_FAILURE_DECIDER
            = (httpParam, httpResult, e) -> Objects.nonNull(e) || !httpResult.isSuccess() || !Objects.equals(httpResult.getCode(), ApiCallResultCode.ApiCallSuccessCode.SUCCESS.getCode());

    /**
     * get请求
     *
     * @param httpParam 调用参数
     */
    public HttpResult get(HttpParam httpParam) {
        return get(httpParam, DEFAULT_API_CALL_FAILURE_DECIDER, DEFAULT_API_CALL_FAILURE_CALL_BACK);
    }

    /**
     * get请求
     *
     * @param httpParam          调用参数
     * @param callFailureDecider 自定义异常决定器
     */
    public HttpResult get(HttpParam httpParam, ApiCallFailureDecider callFailureDecider) {
        return get(httpParam, callFailureDecider, DEFAULT_API_CALL_FAILURE_CALL_BACK);
    }

    /**
     * get请求
     *
     * @param httpParam           调用参数
     * @param callFailureCallback 自定义失败处理器
     */
    public HttpResult get(HttpParam httpParam, ApiCallFailureCallback callFailureCallback) {
        return get(httpParam, DEFAULT_API_CALL_FAILURE_DECIDER, callFailureCallback);
    }

    /**
     * get请求
     *
     * @param httpParam           调用参数
     * @param callFailureDecider  自定义异常决定器
     * @param callFailureCallback 自定义失败处理器
     */
    public abstract HttpResult get(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback);

    /**
     * post请求
     *
     * @param httpParam 调用参数
     */
    public HttpResult post(HttpParam httpParam) {
        return post(httpParam, DEFAULT_API_CALL_FAILURE_DECIDER, DEFAULT_API_CALL_FAILURE_CALL_BACK);
    }

    /**
     * post请求
     *
     * @param httpParam          调用参数
     * @param callFailureDecider 自定义异常决定器
     */
    public HttpResult post(HttpParam httpParam, ApiCallFailureDecider callFailureDecider) {
        return post(httpParam, callFailureDecider, DEFAULT_API_CALL_FAILURE_CALL_BACK);
    }

    /**
     * post请求
     *
     * @param httpParam           参数
     * @param callFailureCallback 自定义失败处理器
     */
    public HttpResult post(HttpParam httpParam, ApiCallFailureCallback callFailureCallback) {
        return post(httpParam, DEFAULT_API_CALL_FAILURE_DECIDER, callFailureCallback);
    }

    /**
     * post请求
     *
     * @param httpParam           调用参数
     * @param callFailureDecider  自定义异常决定器
     * @param callFailureCallback 自定义失败处理器
     */
    public abstract HttpResult post(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback);

    /**
     * delete请求
     *
     * @param httpParam           调用参数
     * @param callFailureDecider  自定义异常决定器
     * @param callFailureCallback 自定义失败处理器
     */
    public abstract HttpResult delete(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback);

    /**
     * patch请求
     *
     * @param httpParam           调用参数
     * @param callFailureDecider  自定义异常决定器
     * @param callFailureCallback 自定义失败处理器
     */
    public abstract HttpResult patch(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback);

}
