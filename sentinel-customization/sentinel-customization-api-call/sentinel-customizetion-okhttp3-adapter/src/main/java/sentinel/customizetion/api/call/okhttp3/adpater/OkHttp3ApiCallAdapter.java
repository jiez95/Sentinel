package sentinel.customizetion.api.call.okhttp3.adpater;


import com.alibaba.csp.sentinel.*;
import org.apache.commons.lang3.StringUtils;
import sentinel.customizetion.api.call.common.AbstractApiCallAdapter;
import sentinel.customizetion.api.call.common.ApiCallFailureCallback;
import sentinel.customizetion.api.call.common.ApiCallFailureDecider;
import sentinel.customizetion.api.call.common.domian.HttpParam;
import sentinel.customizetion.api.call.common.domian.HttpResult;

import java.util.Objects;

/**
 * @author : jiez
 * @date : 2021/2/25 14:25
 */
public class OkHttp3ApiCallAdapter extends AbstractApiCallAdapter {

    @Override
    public HttpResult get(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback) {
        HttpResult result = null;
        Exception existException = null;
        boolean isNeedFailBack = false;
        Entry entry = null;
        try {
            entry = SphU.entry(httpParam.getRequestUrl(), ResourceTypeConstants.COMMON_WEB, EntryType.OUT);
            result = OkHttp3ApiService.getInstance().get(httpParam);
            isNeedFailBack = callFailureDecider.decideFault(httpParam, result, existException);

        } catch (Exception e) {
            existException = e;
            isNeedFailBack = callFailureDecider.decideFault(httpParam, result, existException);

        } finally {
            if (isNeedFailBack) {
                Tracer.traceEntry(existException, entry);
                result = callFailureCallback.callBack(httpParam, result, existException);
            }
            if (Objects.nonNull(entry)) {
                entry.exit();
            }
        }
        return result;
    }

    @Override
    public HttpResult post(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback) {
        String paramArea = StringUtils.isBlank(httpParam.getParamArea()) ? HttpParam.ParamAreaAndFormatConstant.IN_URL_NORMAL : httpParam.getParamArea();
        HttpResult result = null;
        Exception existException = null;
        Entry entry = null;
        boolean isNeedFailBack = false;
        try {
            entry = SphU.entry(httpParam.getRequestUrl(), ResourceTypeConstants.COMMON_WEB, EntryType.OUT);
            switch (paramArea) {
                case HttpParam.ParamAreaAndFormatConstant.IN_URL_NORMAL:
                    result = OkHttp3ApiService.getInstance().postUrl(httpParam);
                    break;
                case HttpParam.ParamAreaAndFormatConstant.IN_FORM_NORMAL:
                    result = OkHttp3ApiService.getInstance().post(httpParam);
                    break;
                case HttpParam.ParamAreaAndFormatConstant.IN_BODY_JSON:
                    result = OkHttp3ApiService.getInstance().postJson(httpParam);
                    break;
                default:
                    break;
            }
            isNeedFailBack = callFailureDecider.decideFault(httpParam, result, existException);
        } catch (Exception e) {
            existException = e;
            isNeedFailBack = callFailureDecider.decideFault(httpParam, result, existException);
        } finally {
            if (isNeedFailBack) {
                Tracer.traceEntry(existException, entry);
                result = callFailureCallback.callBack(httpParam, result, existException);
            }
            if (Objects.nonNull(entry)) {
                entry.exit();
            }
        }
        return result;
    }

    @Override
    public HttpResult delete(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback) {
        return null;
    }

    @Override
    public HttpResult patch(HttpParam httpParam, ApiCallFailureDecider callFailureDecider, ApiCallFailureCallback callFailureCallback) {
        return null;
    }

}
