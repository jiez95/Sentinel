package sentinel.customizetion.api.call.common.domian;

import java.util.Objects;

/**
 * http - result.code
 *
 * @author : jiez
 * @date : 2021/2/26 9:03
 */
public class ApiCallResultCode {

    public enum ApiCallSuccessCode {

        SUCCESS(200,"成功");

        private Integer code;
        private String message;

        ApiCallSuccessCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public enum ApiCallErrorCode {

        TIMEOUT(1001,"Socket超时"),

        IO_ERROR(1002,"调用外部接口异常，请联系IT人员");

        private Integer code;
        private String message;

        ApiCallErrorCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public enum ApiCallLimitCode {

        FLOW_LIMIT_ERROR(1010,"触发流量限制，请稍后重试"),

        OUTER_API_LIMIT_ERROR(1011,"外部接口调用异常超过上限，请联系IT人员");

        private Integer code;
        private String message;

        ApiCallLimitCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    public static boolean isCallApiException(Integer resultCode) {
        for (ApiCallErrorCode resultCodeEnum : ApiCallErrorCode.values()) {
            if (Objects.equals(resultCodeEnum.getCode(), resultCode)) {
                return true;
            }
        }

        for (ApiCallLimitCode limitCode : ApiCallLimitCode.values()) {
            if (Objects.equals(limitCode.getCode(), resultCode)) {
                return true;
            }
        }
        return false;
    }
}