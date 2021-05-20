package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.datasource.WritableDataSource;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.transport.util.WritableDataSourceRegistry;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CtSphTest {

    @Test
    public void testCreateFile() throws IOException {
        String fileName = "D:/sentinel-file/flow-rule.json";
        File file = new File(fileName);
        initFile(file, false);
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

    @Test
    public void testFlowRulePersistenceInFile() {

        Entry entry = null;
        try {
            entry = SphU.entry("test");
        } catch (BlockException e) {
            e.printStackTrace();
            entry.exit();
        }
//        updateFlowRules();

        while (true) {
            System.out.println(JSONObject.toJSONString(FlowRuleManager.getRules()));
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFlowRules() {
        String newFlowRuleJson = "[{\"clusterMode\":false,\"controlBehavior\":0,\"count\":4.0,\"grade\":1,\"limitApp\":\"default\",\"maxQueueingTimeMs\":500,\"resource\":\"https://wwww.baidu.com\",\"strategy\":0,\"warmUpPeriodSec\":10}]";
        List rules = JSONArray.parseArray(newFlowRuleJson, FlowRule.class);
        FlowRuleManager.loadRules(rules);
        AssertUtil.isTrue(this.writeToDataSource(WritableDataSourceRegistry.getFlowDataSource(), rules), "partial success (write data source failed)");
    }

    private <T> boolean writeToDataSource(WritableDataSource<T> dataSource, T value) {
        if (dataSource != null) {
            try {
                dataSource.write(value);
            } catch (Exception e) {
                RecordLog.warn("Write data source failed", e);
                return false;
            }
        }
        return true;
    }

}