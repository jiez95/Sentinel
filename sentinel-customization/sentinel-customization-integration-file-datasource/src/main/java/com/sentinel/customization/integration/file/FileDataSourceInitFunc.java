package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource;
import com.alibaba.csp.sentinel.datasource.FileWritableDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
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

    private FileRefreshableDataSource<List<DegradeRule>> degradeRuleRefreshableDataSource = null;

    private FileRefreshableDataSource<List<SystemRule>> systemRuleRefreshableDataSource = null;

    private FileRefreshableDataSource<List<AuthorityRule>> authorityRuleRefreshableDataSource = null;

    @Override
    public void init() throws Exception {
        initFlowRuleDataSource();
        iniDegradeRuleDataSource();
        iniSystemRuleDataSource();
        iniAuthorityRuleDataSource();
    }

    @SuppressWarnings("uncheck")
    private void initFlowRuleDataSource() throws IOException {
        File file = new File("D:\\sentinel-file\\flow-rule.json");
        initFile(file, false);
        WritableDataSourceRegistry.registerFlowDataSource(new FileWritableDataSource<>(file, JSONObject::toJSONString, Charset.forName("UTF-8")));
        flowRuleRefreshableDataSource = new FileRefreshableDataSource<>(file, source -> JSONArray.parseArray(source, FlowRule.class));
        FlowRuleManager.register2Property(flowRuleRefreshableDataSource.getProperty());
    }

    @SuppressWarnings("uncheck")
    private void iniDegradeRuleDataSource() throws IOException {
        File file = new File("D:\\sentinel-file\\degrade-rule.json");
        initFile(file, false);
        WritableDataSourceRegistry.registerDegradeDataSource(new FileWritableDataSource<>(file, JSONObject::toJSONString, Charset.forName("UTF-8")));
        degradeRuleRefreshableDataSource = new FileRefreshableDataSource<>(file, source -> JSONArray.parseArray(source, DegradeRule.class));
        DegradeRuleManager.register2Property(degradeRuleRefreshableDataSource.getProperty());
    }

    @SuppressWarnings("uncheck")
    private void iniSystemRuleDataSource() throws IOException {
        File file = new File("D:\\sentinel-file\\system-rule.json");
        initFile(file, false);
        WritableDataSourceRegistry.registerSystemDataSource(new FileWritableDataSource<>(file, JSONObject::toJSONString, Charset.forName("UTF-8")));
        systemRuleRefreshableDataSource = new FileRefreshableDataSource<>(file, source -> JSONArray.parseArray(source, SystemRule.class));
        SystemRuleManager.register2Property(systemRuleRefreshableDataSource.getProperty());
    }

    @SuppressWarnings("uncheck")
    private void iniAuthorityRuleDataSource() throws IOException {
        File file = new File("D:\\sentinel-file\\authority-rule.json");
        initFile(file, false);
        WritableDataSourceRegistry.registerAuthorityDataSource(new FileWritableDataSource<>(file, JSONObject::toJSONString, Charset.forName("UTF-8")));
        authorityRuleRefreshableDataSource = new FileRefreshableDataSource<>(file, source -> JSONArray.parseArray(source, AuthorityRule.class));
        AuthorityRuleManager.register2Property(authorityRuleRefreshableDataSource.getProperty());
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
