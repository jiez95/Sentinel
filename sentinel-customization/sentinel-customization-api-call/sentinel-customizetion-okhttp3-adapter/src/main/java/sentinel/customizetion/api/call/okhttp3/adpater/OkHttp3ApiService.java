package sentinel.customizetion.api.call.okhttp3.adpater;


import jdk.nashorn.internal.ir.Assignment;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import sentinel.customizetion.api.call.common.domian.HttpParam;
import sentinel.customizetion.api.call.common.domian.HttpResult;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * http调用接口封装类
 *
 * @author unknow
 */
@SuppressWarnings(value = "unchecked")
public class OkHttp3ApiService {

    private static final String HTTP_URL_ADDRESS_PARAM_SEPARATOR = "?";

    private static final String HTTP_URL_PARAM_LINK_SYMBOLIC = "&";

    private static final String HTTP_URL_PARAM_ASSIGNMENT_SYMBOLIC = "=";

    private volatile static OkHttp3ApiService instance;

    private OkHttpClient okHttpClient;

    private OkHttp3ApiService() {
        okHttpClient = new OkHttpClient.Builder().
                connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized OkHttp3ApiService getInstance() {
        if (instance == null) {
            instance = new OkHttp3ApiService();
        }
        return instance;
    }

    /**
     * get请求
     *
     * @param httpParam 参数
     * @return Result
     */
    public HttpResult get(HttpParam httpParam) throws IOException {
        if (httpParam == null) {
            httpParam = new HttpParam();
        }
        Headers headers = buildHeaders(httpParam);
        Request request = new Request.Builder().url(buildUrlParams(httpParam.getRequestUrl(), httpParam.getParams()))
                .headers(headers)
                .get().build();
        return doCallApi(request);
    }

    /**
     * Post请求
     *
     * @param httpParam 参数
     * @return Result
     */
    public HttpResult post(HttpParam httpParam) throws IOException {
        if (httpParam == null) {
            throw new RuntimeException("参数异常");
        }
        Headers headers = buildHeaders(httpParam);
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        buildFormBody(httpParam, formBodyBuilder);
        Request request = new Request.Builder().url(httpParam.getRequestUrl())
                .headers(headers)
                .post(formBodyBuilder.build()).build();
        return doCallApi(request);
    }

    /**
     * post请求，参数放URL
     *
     * @param httpParam 参数
     * @return Result
     */
    public HttpResult postUrl(HttpParam httpParam) throws IOException {
        if (httpParam == null) {
            httpParam = new HttpParam();
        }
        Headers headers = buildHeaders(httpParam);
        Request request = new Request.Builder().url(buildUrlParams(httpParam.getRequestUrl(), httpParam.getParams()))
                .headers(headers)
                .post(new FormBody.Builder().build()).build();
        return doCallApi(request);
    }


    /**
     * postForm json方式传送数据
     *
     * @param httpParam 参数
     * @return Result
     */
    public HttpResult postJson(HttpParam httpParam) throws IOException {
        if (httpParam == null) {
            httpParam = new HttpParam();
        }
        Headers headers = buildHeaders(httpParam);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), httpParam.getRequestBody());
        Request request = new Request.Builder()
                .url(httpParam.getRequestUrl())
                .headers(headers)
                .post(requestBody)
                .build();
        return doCallApi(request);
    }

    /**
     * 解析参数信息
     *
     * @param httpParam HttpParam
     * @return okHttp3#Headers
     */
    private Headers buildHeaders(HttpParam httpParam) {
        Headers.Builder headerBuilder = new Headers.Builder();

        //设置验证信息
        if (StringUtils.isNotBlank(httpParam.getUsername())) {
            String authBase = Base64.getEncoder().encodeToString((httpParam.getUsername() + ":" + httpParam.getPassword()).getBytes());
            headerBuilder.add("Authorization", "Basic " + authBase);
        }

        //设置其他header
        if (httpParam.getHeader() != null && httpParam.getHeader().size() > 0) {
            Iterator<String> iterator = httpParam.getHeader().keySet().iterator();
            String key = "";
            while (iterator.hasNext()) {
                key = iterator.next();
                headerBuilder.add(key, httpParam.getHeader().get(key));
            }
        }
        return headerBuilder.build();
    }

    /**
     * 构建Url参数
     *
     * @param url    调用URL
     * @param params 参数map
     * @return Url参数参数
     */
    private String buildUrlParams(String url, Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(params) && params.size() > 0) {
            if (url.contains(HTTP_URL_ADDRESS_PARAM_SEPARATOR)) {
                sb.append(HTTP_URL_PARAM_LINK_SYMBOLIC);
            } else {
                sb.append(HTTP_URL_ADDRESS_PARAM_SEPARATOR);
            }

            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                if (Objects.nonNull(entry.getValue())) {
                    sb.append(entry.getKey());
                    sb.append(HTTP_URL_PARAM_ASSIGNMENT_SYMBOLIC);
                    try {
                        sb.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    sb.append(HTTP_URL_PARAM_LINK_SYMBOLIC);
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            return url + sb.toString();
        }
        return url;
    }

    /**
     * 构建表单参数
     *
     * @param httpParam       参数
     * @param formBodyBuilder okHttp3Body构建器
     */
    private void buildFormBody(HttpParam httpParam, FormBody.Builder formBodyBuilder) {

        if (httpParam.getParams() != null && httpParam.getParams().size() > 0) {
            Set<Map.Entry<String, String>> entrySet = httpParam.getParams().entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                formBodyBuilder.add(entry.getKey(), StringUtils.isBlank(entry.getValue()) ? "" : entry.getValue());
            }
        }
    }

    /**
     * 真实调用API操作
     *
     * @param request
     * @return
     */
    private HttpResult doCallApi(Request request) throws IOException {
        Call call = okHttpClient.newCall(request);
        Response response = call.execute();
        String bodyMsg = response.body().string();
        if (response.isSuccessful()) {
            return HttpResult.success(response.message(), bodyMsg);
        }
        return new HttpResult(response.code(), response.message(), bodyMsg, false);
    }
}