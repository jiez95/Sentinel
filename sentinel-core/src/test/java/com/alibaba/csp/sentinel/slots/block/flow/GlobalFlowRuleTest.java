package com.alibaba.csp.sentinel.slots.block.flow;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.ResourceTypeConstants;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.AssertUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author : jiez
 * @date : 2021/7/1 9:04
 */
public class GlobalFlowRuleTest {

    @Test
    public void testGlobalFlowRuleLimit() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("testA");
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setGlobalMode(true);
        flowRule.setClusterMode(true);
        flowRule.setLimitApp("default");
        FlowRuleManager.loadRules(Arrays.asList(flowRule));

        AssertUtil.isTrue(FlowRuleManager.getGlobalRules().size() <= 0, "全局流控配置限制失效");
    }

    @Test
    public void testGlobalFlowRuleLoadAndSort() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("testA");
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setGlobalMode(true);
        flowRule.setLimitApp("default");
        FlowRuleManager.loadRules(Arrays.asList(flowRule));

        AssertUtil.isTrue(FlowRuleManager.getGlobalRules().size() == 1, "全局流控配置限制加载异常");
        AssertUtil.isTrue(FlowRuleManager.getRules().size() <= 0, "全局流控配置限制加载异常");
    }

    @Test
    public void testGlobalFlowRuleWithDefaultOrigin() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("testA");
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setGlobalMode(true);
        flowRule.setLimitApp("default");
        FlowRuleManager.loadRules(Arrays.asList(flowRule));

        boolean isSuccess = false;
        mockData mockData = mockEntry(2, "testA");

        AssertUtil.isTrue(Objects.nonNull(mockData.getExecTimes()) && mockData.getExecTimes() == 1,
                "全局配置失效, 没有正常拦截");
        AssertUtil.isTrue(Objects.nonNull(mockData.getExecException()) && mockData.getExecException() instanceof FlowException,
                "全局配置失效, 没有正常拦截");
    }

    @Test
    public void testGlobalFlowRuleWithDiffOrigin() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("testA");
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setGlobalMode(true);
        flowRule.setLimitApp("B");
        FlowRuleManager.loadRules(Arrays.asList(flowRule));

        boolean isSuccess = false;
        ContextUtil.enter("testContext", "A");
        mockData mockData = mockEntry(2, "testA");

        AssertUtil.isTrue(Objects.nonNull(mockData.getExecTimes()) && mockData.getExecTimes() == 2,
                "全局配置失效, 没有正常拦截");
        AssertUtil.isTrue(Objects.isNull(mockData.getExecException()),
                "全局配置失效, 没有正常拦截");
    }

    @Test
    public void testGlobalFlowRuleWithSameOrigin() {
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("testA");
        flowRule.setCount(1);
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setGlobalMode(true);
        flowRule.setLimitApp("A");
        FlowRuleManager.loadRules(Arrays.asList(flowRule));

        boolean isSuccess = false;
        ContextUtil.enter("testContext", "A");
        mockData mockData = mockEntry(2, "testA");

        AssertUtil.isTrue(Objects.nonNull(mockData.getExecTimes()) && mockData.getExecTimes() == 1,
                "全局配置失效, 没有正常拦截");
        AssertUtil.isTrue(Objects.nonNull(mockData.getExecException()) && mockData.getExecException() instanceof FlowException,
                "全局配置失效, 没有正常拦截");
    }

    private mockData mockEntry(int targetExecTimes, String resourceName) {
        int execTimes = 0;
        Exception exception = null;
        try {
            for (int i = 0; i < targetExecTimes; i++) {
                Entry testA = SphU.entry(resourceName, ResourceTypeConstants.COMMON_WEB, EntryType.OUT);
                testA.exit();
                execTimes++;
            }
        } catch (Exception e) {
            exception = e;
        }
        return new mockData(execTimes, exception);
    }
}

class mockData {

    private Integer execTimes;

    private Exception execException;

    public mockData() {
    }

    public mockData(Integer execTimes, Exception execException) {
        this.execTimes = execTimes;
        this.execException = execException;
    }

    public Integer getExecTimes() {
        return execTimes;
    }

    public void setExecTimes(Integer execTimes) {
        this.execTimes = execTimes;
    }

    public Exception getExecException() {
        return execException;
    }

    public void setExecException(Exception execException) {
        this.execException = execException;
    }
}
