package sentinel.customizetion.api.call.common.domian;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * call http result
 *
 * @author : jiez
 * @date : 2021/2/26 9:03
 */
@SuppressWarnings(value = "unchecked")
public class HttpResult<T> {

    /**
     * 状态码
     **/
    private Integer code;

    /**
     * 反馈信息
     **/
    private String msg;

    /**
     * 返回数据
     **/
    private T data;

    /**
     * 返回成功与否状态
     */
    private boolean success;

    public HttpResult(Integer code, String msg, T data, boolean success) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.success = success;
    }

    /**
     * 成功返回
     *
     * @param responseData 返回数据
     */
    public static HttpResult success(String originMsg, Object responseData) {
        return new HttpResult(ApiCallResultCode.ApiCallSuccessCode.SUCCESS.getCode(), originMsg, responseData, true);
    }

    /**
     * api调用异常错误返回
     *
     * @param errorCode 调用异常状态Code
     * @see ApiCallResultCode
     */
    public static HttpResult apiError(ApiCallResultCode.ApiCallErrorCode errorCode) {
        return apiError(errorCode, null);
    }


    /**
     * api调用异常错误返回
     *
     * @param errorCode 调用异常状态Code
     * @param object    返回数据
     * @see ApiCallResultCode
     */
    public static HttpResult apiError(ApiCallResultCode.ApiCallErrorCode errorCode, Object object) {
        return new HttpResult(errorCode.getCode(), errorCode.getMessage(), object, false);
    }


    /**
     * api限制/熔断错误返回
     *
     * @param errorCode 调用异常状态Code
     * @see ApiCallResultCode
     */
    public static HttpResult limitError(ApiCallResultCode.ApiCallLimitCode errorCode) {
        return limitError(errorCode, null);
    }

    /**
     * api限制/熔断错误返回
     *
     * @param errorCode 调用异常状态Code
     * @param object    返回数据
     * @see ApiCallResultCode
     */
    public static HttpResult limitError(ApiCallResultCode.ApiCallLimitCode errorCode, Object object) {
        return new HttpResult(errorCode.getCode(), errorCode.getMessage(), object, false);
    }

    /**
     * 错误返回
     *
     * @param statusCode 调用异常状态Code
     * @see ApiCallResultCode
     */
    public static HttpResult error(Integer statusCode, String msg) {
        return new HttpResult(statusCode, msg, null, false);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
