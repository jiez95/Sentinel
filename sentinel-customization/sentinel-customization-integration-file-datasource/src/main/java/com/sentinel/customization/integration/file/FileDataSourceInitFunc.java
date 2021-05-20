package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource;
import com.alibaba.csp.sentinel.datasource.FileWritableDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.spi.SpiLoader;
import com.alibaba.csp.sentinel.transport.util.WritableDataSourceRegistry;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * @author : jiez
 * @date : 2021/5/15 16:44
 */
public class FileDataSourceInitFunc implements InitFunc {

    private FileRefreshableDataSource<List<FlowRule>> flowRuleRefreshableDataSource = null;

    @Override
    public void init() throws Exception {
        initFlowRuleDataSource();
    }

    @SuppressWarnings("uncheck")
    private void initFlowRuleDataSource() throws IOException {
        File file = new File("D:\\sentinel-file\\flow-rule.json");
        initFile(file, false);
        WritableDataSourceRegistry.registerFlowDataSource(new FileWritableDataSource<>(file, JSONObject::toJSONString, Charset.forName("UTF-8")));
        flowRuleRefreshableDataSource = new FileRefreshableDataSource<>(file, source -> JSONArray.parseArray(source, FlowRule.class));
        FlowRuleManager.register2Property(flowRuleRefreshableDataSource.getProperty());
    }

    private void initFile(File file, boolean isCreateDirectory) throws IOException {
        if (file.exists()) {
            return;
        }
        File parentFile = file.getParentFile();
        if (Objects.nonNull(parentFile)) {
            if (!parentFile.exists()) {
                initFile(parentFile, true);
            }
        }
        if (isCreateDirectory) {
            file.mkdir();
        } else {
            file.createNewFile();
        }
    }


}
