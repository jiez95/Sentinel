package sentinel.customizetion.api.call.common.domian;
import java.util.HashMap;
import java.util.Map;

/**
 * call http param
 *
 * @author : jiez
 * @date : 2021/2/26 9:03
 */
public class HttpParam {

    public static class ParamAreaAndFormatConstant {
        public static final String IN_URL_NORMAL = "in_url";
        public static final String IN_FORM_NORMAL = "in_form";
        public static final String IN_BODY_JSON = "in_body_json";
    }

    private Map<String, String> header = new HashMap<>();

    private Map<String, String> params = new HashMap<>();

    private String requestUrl;

    private String paramArea = ParamAreaAndFormatConstant.IN_URL_NORMAL;

    private String requestMethod;

    private String requestBody;

    private String username;

    private String password;

    public static HttpParam builderGetParam(String url) {
        return builderGetParam(url, null, null);
    }

    public static HttpParam builderGetParam(String url, Map<String, String> params) {
        return builderGetParam(url, null, params);
    }

    public static HttpParam builderGetParam(String url, Map<String, String> header, Map<String, String> params) {
        HttpParam httpParam = new HttpParam();
        httpParam.setRequestUrl(url);
        httpParam.setHeader(header);
        httpParam.setParams(params);
        return httpParam;
    }

    public static HttpParam builderPostJsonParam(String url, String requestBody) {
        return builderPostJsonParam(url, null, requestBody);
    }

    public static HttpParam builderPostJsonParam(String url, Map<String, String> header, String requestBody) {
        HttpParam httpParam = new HttpParam();
        httpParam.setRequestUrl(url);
        httpParam.setHeader(header);
        httpParam.setParamArea(ParamAreaAndFormatConstant.IN_BODY_JSON);
        httpParam.setRequestBody(requestBody);
        return httpParam;
    }

    public void putHeader(String key, String value) {
        this.header.put(key, value);
    }

    public void putParams(String key, String value) {
        this.params.put(key, value);
    }

    public Map<String, String> getHeader() {
        return header;
    }

    private void setHeader(Map<String, String> header) {
        this.header = header;
    }

    public Map<String, String> getParams() {
        return params;
    }

    private void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getParamArea() {
        return paramArea;
    }

    public void setParamArea(String paramArea) {
        this.paramArea = paramArea;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
