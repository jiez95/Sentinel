package sentinel.customizetion.api.call.common.domian;


import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author : jiez
 * @date : 2021/3/26 16:23
 */
public class ApiBaseResultErrorInfo {

    @JSONField(name = "code")
    private Integer code;

    @JSONField(name = "details")
    private String details;

    @JSONField(name = "error_lang")
    private String errorLang;

    @JSONField(name = "msg")
    private String message;

    @JSONField(name = "validation_error_info")
    private String validationErrorInfo;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getErrorLang() {
        return errorLang;
    }

    public void setErrorLang(String errorLang) {
        this.errorLang = errorLang;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getValidationErrorInfo() {
        return validationErrorInfo;
    }

    public void setValidationErrorInfo(String validationErrorInfo) {
        this.validationErrorInfo = validationErrorInfo;
    }
}


