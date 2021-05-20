package sentinel.customizetion.api.call.common.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * javaConfig - sentinel.flow.rule.config
 *
 * @author : jiez
 * @date : 2021/2/26 9:03
 */
public class SentinelRuleConfig {

    static {
        initQpsFlowRule();
    }

    private static void initQpsFlowRule() {
        List<FlowRule> rules = new ArrayList<FlowRule>();
        FlowRule rule1 = new FlowRule();
        rule1.setResource("https://wwww.baidu.com");
        // set limit qps to 20
        rule1.setCount(1);
        rule1.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule1.setLimitApp("default");
        rules.add(rule1);
        FlowRuleManager.loadRules(rules);
    }

}
