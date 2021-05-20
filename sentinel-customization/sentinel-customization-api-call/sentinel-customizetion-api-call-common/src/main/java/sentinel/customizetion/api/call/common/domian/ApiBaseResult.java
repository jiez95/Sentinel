package sentinel.customizetion.api.call.common.domian;

import com.alibaba.fastjson.annotation.JSONField;


/**
 * @author : jiez
 * @date : 2021/3/26 16:19
 */
public class ApiBaseResult {

    @JSONField(name = "error_info")
    private ApiBaseResultErrorInfo errorInfo;

    @JSONField(name = "is_authorized_request")
    private Boolean authorizedRequest;

    @JSONField(name = "is_success")
    private Boolean success;

    @JSONField(name = "result")
    private Object result;

    @JSONField(name = "target_url")
    private String targetUrl;

    public ApiBaseResultErrorInfo getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(ApiBaseResultErrorInfo errorInfo) {
        this.errorInfo = errorInfo;
    }

    public Boolean getAuthorizedRequest() {
        return authorizedRequest;
    }

    public void setAuthorizedRequest(Boolean authorizedRequest) {
        this.authorizedRequest = authorizedRequest;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}

