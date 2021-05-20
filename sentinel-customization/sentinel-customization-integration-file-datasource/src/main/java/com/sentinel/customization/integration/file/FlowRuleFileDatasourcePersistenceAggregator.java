package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileWritableDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.transport.util.WritableDataSourceRegistry;
import com.alibaba.fastjson.JSON;
import com.sentinel.customization.integration.file.AbstractFileDatasourcePersistenceAggregator;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author : jiez
 * @date : 2021/5/15 20:45
 */
public class FlowRuleFileDatasourcePersistenceAggregator extends AbstractFileDatasourcePersistenceAggregator {

    private static final String FLOW_RULE_FILE_PATH = "";

    @Override
    public String getFileName() {
        return "G:\\sentinel-file\\flow-rule.json";
    }

    @Override
    public Converter getConfigEncoder() {
        return (Converter<String, List<FlowRule>>) source -> null;
    }

    @Override
    public Charset getCharset() {
        return Charset.forName("UTF-8");
    }

    @Override
    public void registerToWritableDataSourceRegistry(FileWritableDataSource fileWritableDataSource) {
        WritableDataSourceRegistry.registerFlowDataSource(fileWritableDataSource);
    }
}
