/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.context.NullContext;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.IntervalProperty;
import com.alibaba.csp.sentinel.slotchain.*;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthoritySlot;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeSlot;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleChecker;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowSlot;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.logger.LogSlot;
import com.alibaba.csp.sentinel.slots.statistic.StatisticSlot;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemSlot;
import com.alibaba.csp.sentinel.slots.system.SystemStatusListener;

/**
 * {@inheritDoc}
 *
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 * @see Sph
 */
public class CtSph implements Sph {

    private static final Object[] OBJECTS0 = new Object[0];

    /**
     * Same resource({@link ResourceWrapper#equals(Object)}) will share the same
     * {@link ProcessorSlotChain}, no matter in which {@link Context}.
     */
    private static volatile Map<ResourceWrapper, ProcessorSlotChain> chainMap
        = new HashMap<ResourceWrapper, ProcessorSlotChain>();

    private static final Object LOCK = new Object();

    private AsyncEntry asyncEntryWithNoChain(ResourceWrapper resourceWrapper, Context context) {
        AsyncEntry entry = new AsyncEntry(resourceWrapper, null, context);
        entry.initAsyncContext();
        // The async entry will be removed from current context as soon as it has been created.
        entry.cleanCurrentEntryInLocal();
        return entry;
    }

    private AsyncEntry asyncEntryWithPriorityInternal(ResourceWrapper resourceWrapper, int count, boolean prioritized,
                                                      Object... args) throws BlockException {
        Context context = ContextUtil.getContext();
        if (context instanceof NullContext) {
            // The {@link NullContext} indicates that the amount of context has exceeded the threshold,
            // so here init the entry only. No rule checking will be done.
            return asyncEntryWithNoChain(resourceWrapper, context);
        }
        if (context == null) {
            // Using default context.
            context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
        }

        // Global switch is turned off, so no rule checking will be done.
        if (!Constants.ON) {
            return asyncEntryWithNoChain(resourceWrapper, context);
        }

        ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);

        // Means processor cache size exceeds {@link Constants.MAX_SLOT_CHAIN_SIZE}, so no rule checking will be done.
        if (chain == null) {
            return asyncEntryWithNoChain(resourceWrapper, context);
        }

        AsyncEntry asyncEntry = new AsyncEntry(resourceWrapper, chain, context);
        try {
            chain.entry(context, resourceWrapper, null, count, prioritized, args);
            // Initiate the async context only when the entry successfully passed the slot chain.
            asyncEntry.initAsyncContext();
            // The asynchronous call may take time in background, and current context should not be hanged on it.
            // So we need to remove current async entry from current context.
            asyncEntry.cleanCurrentEntryInLocal();
        } catch (BlockException e1) {
            // When blocked, the async entry will be exited on current context.
            // The async context will not be initialized.
            asyncEntry.exitForContext(context, count, args);
            throw e1;
        } catch (Throwable e1) {
            // This should not happen, unless there are errors existing in Sentinel internal.
            // When this happens, async context is not initialized.
            RecordLog.warn("Sentinel unexpected exception in asyncEntryInternal", e1);

            asyncEntry.cleanCurrentEntryInLocal();
        }
        return asyncEntry;
    }

    private AsyncEntry asyncEntryInternal(ResourceWrapper resourceWrapper, int count, Object... args)
        throws BlockException {
        return asyncEntryWithPriorityInternal(resourceWrapper, count, false, args);
    }

    private Entry entryWithPriority(ResourceWrapper resourceWrapper, int count, boolean prioritized, Object... args)
        throws BlockException {
        // 获取当前线程上下文是否有配置Context
        Context context = ContextUtil.getContext();
        /**
         * 当线程上下文资源 大于 最大限制上下文数量（2000）时 context就是为NullContext
         */
        if (context instanceof NullContext) {
            // The {@link NullContext} indicates that the amount of context has exceeded the threshold,
            // so here init the entry only. No rule checking will be done.
            return new CtEntry(resourceWrapper, null, context);
        }

        /**
         * 如果没有手动设置上下文, 就是使用默认上下文（sentinel_default_context）
         * 手动设置方式:
         *      ContextUtil#trueEnter(String name, String origin)
         *      name: 上下文名
         *      origin: 来源
         */
        if (context == null) {
            // Using default context.
            context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
        }

        // Global switch is close, no rule checking will do.
        // 看全局控制是否关闭流控了
        if (!Constants.ON) {
            return new CtEntry(resourceWrapper, null, context);
        }

        /**
         * 找寻处理链
         *  1. 如果资源数大于6000，会拒绝继续统计资源(chain == null)
         *  2. 处理链是跟随资源的，即相同资源用同一个处理链，不同资源用不同的处理链
         *  3. 是利用SPI加载的
         */
        ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);

        /*
         * Means amount of resources (slot chain) exceeds {@link Constants.MAX_SLOT_CHAIN_SIZE},
         * so no rule checking will be done.
         */
        /**
         * 资源超出限制, 不做限流处理
         */
        if (chain == null) {
            return new CtEntry(resourceWrapper, null, context);
        }

        /**
         * 新建入口资源链路Entry(Entry-A)
         *
         * 如果当前线程上下文已经有入口资源链路Entry(Entry-B)【链路头结点】，把 Entry-A 连到 Entry-B后面
         * 如果当前线程上下文没有入口资源链路Entry【即没有链路头结点】，就把Entry-A设置到线程上下文中成为入口资源链路头结点
         */
        Entry e = new CtEntry(resourceWrapper, chain, context);

        try {
            /**
             * 线程上下结构
             *
             * Context
             *      name(String) -> 当前上下文名
             *      entranceNode(EntranceNode) -> 当前上下文入口节点类
             *      curEntry(CtEntry) -> 当前上下文链路条目(链表)第一个节点
             *      origin(String) -> 来源
             *      async(boolean) -> 是否异步
             */

            /**
             * 开始执行槽链逻辑
             *
             * 处理槽链执行顺序:
             *
             * - 默认处理槽
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 作为链路的抽象入口，调度第一个节点的transformEntry方法(流转到下个节点)
             *
             *  Slot 资源管理结束 关键点:
             *      1. 无逻辑
             *
             * @see DefaultProcessorSlotChain
             *
             *
             * - 节点选择槽(责任链的起点)
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 这个slot获取/生成一个DefaultNode，这个DefaultNode
             *
             *      2. 获取方式:
             *          以目标访问资源(resourceWrapper)作为维度的
             *              ，不同Context 的 相同访问资源(resourceWrapper)会获取到不同的节点
             *          即以Context + 目标资源名 作为唯一识别值.
             *          (根据 目标资源名 获取 slot chain， slot chain中的NodeSelectorSlot 根据 Context 获取 DefaultNode)
             *          资源名 : slot chain = 1 : 1
             *          Context : DefaultNode = 1 : 1
             *          资源名 : Context = 1 : N
             *          资源名 : DefaultNode = 1 : N
             *
             *      3. 把获取到的 这个DefaultNode 赋值给 Context#CurNode
             *
             *      3. （猜测未证实）DefaultNode extends StatisticNode，因此DefaultNode是有统计功能
             *          ，而 DefaultNode 标识的是 某个Context 对 某个资源 的 访问
             *          ，因此 DefaultNode 记录的是 某个Context 对 某个资源的 的 流量统计
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑
             *
             * @see com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot
             * @see DefaultNode
             *
             *
             * - 集群生成槽
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 资源名 : clusterNode(资源集群节点) = 1 : 1
             *
             *      2. clusterNode 维护了 一个Map(Map<String, StatisticNode> originCountMap)
             *          , 记录了不同来源的StatisticNode
             *
             *      3. 如果Context访问来源非默认值("")-即指定了访问来源【ContextUtil.enter("context-name", "origin")】
             *          , 则需要从 clusterNode(资源集群节点)-originCountMap 中
             *              获取/创建 Origin统计节点 赋值给 Context#CurNode(DefaultNode)#OriginNode
             *
             *      4. 把获取到的 clusterNode 节点 复制给 DefaultNode#ClusterNode
             *
             *      5.（猜测未证实）ClusterNode extends StatisticNode，因此ClusterNode是有统计功能
             *          , 而 ClusterNode 标识的是 所有Context(甚至可能有多节点统计) 对 某个资源的访问
             *          , 因此 ClusterNode 记录的是 某个资源(所有Context, 甚至多节点)的 流量统计
             *
             *      6. （猜测未证实）originNode 是 StatisticNode，因此originNode是有统计功能
             *          ，而 originNode 标识的是 某个来源 对 某个资源的访问
             *          , 因此 originNode 记录的是 某个来源 对 某个资源的 流量统计
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑的
             *
             * @see ClusterBuilderSlot
             * @see ClusterNode
             * @see ClusterNode#Map<String, StatisticNode> originCountMap
             *
             *
             * - 日志输出槽
             *  属性:
             *      后置处理槽(先走责任链往下处理 再执行自己逻辑)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 仅当出现异常的时候才会输出日志(BlockException(流控异常) / 其他异常)
             *
             *      2. 如果是BlockException，即是Sentinel自定义的异常，会继续往上抛
             *
             *      3. 如果是非BlockException，会把异常吃掉并且输出日志
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑的
             *
             * @see LogSlot
             *
             *
             * - 资源流量统计槽
             *  属性:
             *      后置处理槽(先走责任链往下处理 再执行自己逻辑)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 无异常流程
             *          1.1 维护DefaultNode&ClusterNode的线程数/通过请求数
             *          1.2 如果当前Context是非默认来源，维护当前Entry的originNode的线程数/通过请求数
             *          1.3 如果当前访问类型是EntryType.IN（本系统被访问）
             *              维护系统节点Constants.ENTRY_NODE的线程数/通过请求数
             *          1.4 回调ProcessorSlotEntryCallback#onPass
             *
             *      2. 出现PriorityWaitException(优先资源请求-等待成功)
             *          2.1 维护DefaultNode&ClusterNode的线程数
             *          2.2 如果当前Context是非默认来源，维护当前Entry的originNode的线程数
             *          2.3 如果当前访问类型是EntryType.IN（本系统被访问）
             *              维护系统节点Constants.ENTRY_NODE的线程数
             *          2.4 回调ProcessorSlotEntryCallback#onPass
             *
             *      3. 出现BlockException(资源请求被拦截)
             *          3.1 维护当前Entry的BlockError信息
             *          3.2 如果当前Context是非默认来源，维护当前Entry的originNode的拦截请求数
             *          3.3 如果当前访问类型是EntryType.IN（本系统被访问）
             *              维护系统节点Constants.ENTRY_NODE的拦截请求数
             *          3.4 回调ProcessorSlotEntryCallback#onBlocked
             *          3.5 把异常往上抛
             *
             *      4. 出现 非PriorityWaitException 和 非BlockException 的异常
             *          4.1 维护当前Entry的Eroor信息(服务于 降级slot# 异常比例 & 异常数)
             *
             *  Slot 资源管理结束 关键点:
             *      1. 如果当前Entry有BlockError 只执行回调ProcessorSlotExitCallback
             *      2. 如果当前Entry没有BlockError
             *          1. 计算RT
             *          2. 维护DefaultNode&ClusterNode的RT/异常数
             *          3. 回调ProcessorSlotExitCallback
             *
             * @see StatisticSlot
             *
             *
             * - 系统资源控制槽(全局的，即所有上下文所有资源通用)
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 只针对处理 EntryType.IN 的资源(被别人调用的资源), 即保证自己的系统不被别人调爆
             *
             *      2. 有个全局开关com.alibaba.csp.sentinel.slots.system.SystemRuleManager#checkSystemStatus, 默认是false
             *
             *      3. 全局维护一个 统计节点 Constants.ENTRY_NODE
             *
             *      4. 检查: QPS/当前线程数/平均响应时间/系统负载/cpu使用率
             *
             *      5. 系统资源获取实现方法
             *          OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
             *          osBean.getSystemLoadAverage();
             *          osBean.getSystemCpuLoad();
             *
             *      6. 系统资源控制槽 判断是要进行系统资源限制 则抛出 SystemBlockException
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑的
             *
             * @see SystemSlot
             * @see SystemRuleManager#checkSystem
             * @see SystemStatusListener 查看系统负载实现类
             *
             *
             * - 权限控制槽
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 以访问资源名 + 访问来源为维度设置规则(AuthorityRule)
             *
             *      2. 判断当前资源访问来源 是否 符合规则限制来源的设计
             *          先进行粗略版的筛选: 字符串 indexof匹配
             *          再进行精细版的筛选: 字符串切割匹配
             *
             *      3. 判断访问资源的来源是否在 黑名单/白名单
             *          如果设置了黑名单, 如果来源在黑名单里面, 抛出AuthorityException
             *          如果设置了白名单, 如果来源不在白名单里面, 抛出AuthorityException
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑的
             *
             * @see AuthoritySlot
             * @see AuthorityRuleChecker 判断器
             *
             *
             * - 流量控制槽
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      1. 流控规则 是以 访问资源 为维度来设置的
             *
             *      2. 流控规则的限制来源不能是null
             *
             *      3. 集群版流控
             *          TODO 集群版学习
             *
             *      4. 单机版流控
             *          4.1 sentinel支持多种流控节点限制模式（直接/关联/链路）, 不同的限制模式会判断不同的 流量资源统计节点
             *              因此 第一步 是 根据 流控模型 + 规则设置的来源 来选择使用哪个流量资源统计节点 进行流量判断
             *
             *              直接模型
             *                  1. 流控规则 设置的 限制来源 是 自定义字符串(不为 default/other)
             *                      当前上下文来源 与 设置值 一致
             *                          使用 当前Entry的来源节点(Context#curEntry.getOriginNode()) 作为 流量资源统计节点
             *
             *                      当前上下文来源 与 设置值 不一致
             *                          不做流控限制
             *
             *                  2. 流控规则 设置的 限制来源 是 default
             *                      使用 当前访问资源的集群资源统计节点(resource#ClusterNode)
             *
             *                  3. 流控规则 设置的 限制来源 是 other
             *                      使用 当前Entry的来源节点(Context#curEntry.getOriginNode()) 作为 流量资源统计节点
             *
             *                  4. 流控规则 设置的 限制来源 是 null
             *                      不做流控限制
             *
             *              关联模式
             *                  根据 配置设置的关联资源名 获取 对应的 资源集群节点(ClusterNode) 进行流量判断
             *
             *              链路模式
             *                  仅当 当前上下文名字(Context#name) 与 配置设置的关联资源名 相同时 才做 流量判断
             *                  （这里跟Context节点树有关）
             *
             *          4.2 流控效果控制器（TrafficShapingController）
             *              4.2.1 根据 阈值类型 + 流控效果 决定使用哪个 流控效果控制器
             *                  (决定方法入口: com.alibaba.csp.sentinel.slots.block.flow.FlowRuleUtil#generateRater)
             *
             *                   阈值类型 - QPS
             *                      （可以选择4种流控效果控制器）
             *                      1. WarmUpController(warm up（预热模式）)
             *                      2. RateLimiterController(排队等待)
             *                      3. WarmUpRateLimiterController
             *                      0. DefaultController(快速失败)
             *
             *                   阈值类型 - 线程数
             *                      (只能选一种)
             *                      0. DefaultController(快速失败)
             *
             *
             *      5. prioritized参数
             *          标识此次资源进入是否是优先执行的，这里涉及占用未来窗口令牌数
             *          如果占用未来窗口的令牌成功会返回 PriorityWaitException
             *
             *      6. 如果流量控制槽 判断是要进行流量限制 则抛出 FlowException
             *
             *  Slot 资源管理结束 关键点:
             *      1.无逻辑的
             *
             * @see FlowSlot
             * @see FlowRuleChecker 流量规则判断器
             * @see FlowRuleManager  流量规则管理器
             *
             *
             * - 降级控制槽
             *  属性:
             *      前置处理槽(先执行自己逻辑 再走责任链往下处理)
             *
             *  Slot 资源管理开始 关键点：
             *      新逻辑(1.8.0之后):
             *          以断路器作为维度， 遍历断路器。
             *          把降级规则 映射成一个 断路器. 映射入口:
             *              DynamicSentinelProperty#updateValue
             *                  -> PropertyListener#configUpdate
             *                      -> DegradeRuleManager.RulePropertyListener#configUpdate(java.util.List)
             *                          -> DegradeRuleManager.RulePropertyListener#buildCircuitBreakers(java.util.List)
             *
             *          1. 断路器维护了一个状态(AtomicReference<State> currentState)
             *              OPEN(断路器开启状态)
             *              HALF_OPEN(断路器半开启状态)
             *              CLOSED(断路器关闭状态)
             *
             *          2. 断路器开启一段时间后（断路周期）会有一个重试恢复机制（把断路器状态修改为半开）
             *              半开状态：允许 修改断路器状态(OPEN -> HALF_OPEN)成功的 线程 通过熔断，进行实际业务操作.
             *                  而 其他线程 即时在重试恢复时期 依然是直接熔断
             *
             *          3. 在 方法开启方法（DegradeSlot#entry(...)），熔断降级Slot只是单纯依据熔断器状态判断是否需要熔断，
             *             而在 方法结束方法(DegradeSlot#exit(...)) 判断什么时候需要断路器进行熔断（即修改断路器状态）
             *
             *          4. 断路器 - 方法结束逻辑
             *              1. 如果 当前上下文的当前入口节点(context.getCurEntry()) 存在 拦截异常(getBlockError)， 跳过不处理
             *
             *              2. 后面根据不同的熔断器做不同逻辑
             *                  异常数熔断器:
             *                      判断entry是否有异常信息(setError), 进行数据维护，然后进行计算维护熔断器状态
             *                  RT熔断器：
             *                      统计实际业务处理时间, 然后进行计算维护熔断器状态
             *                      由于需要一个开始时间, 这个开始时间维护是在CtEntry的构建方法里面就已经设置了（即从进入那刻就开始计算时间了）
             *
             *              3. 涉及 时间窗算法
             *
             *              4. 有一个最少请求数保护
             *
             *      旧逻辑(1.8.0之前):
             *          以规则作为维度，遍历规则。
             *
             *          1. 以 访问资源对应的降级槽 作为维度 维护一个变量: AtomicBoolean cut 标识 熔断是否开启
             *
             *          2. 使用 访问资源的集群统计节点 作为 数据来源
             *
             *          3. 有最小请求数保护, 即 请求数 达到一定量(设置值)后 才会进行熔断
             *
             *          4. 熔断规则
             *              0. RT(RuleConstant.DEGRADE_GRADE_RT)
             *              1. 异常比例(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
             *              2. 异常数(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT)
             *
             *          5. 当触发熔断降级后
             *              更新 熔断标识 为 true
             *              开启一个延时任务(时间窗口时间)【用于重置熔断标识】
             *              抛出 DegradeException
             *
             *  Slot 资源管理结束 关键点:
             *      1. 如果当前Entry是被拦截的(有BlockError)，则不作处理
             *      2. 如果当前Entry没有被拦截的(没有BlockError)
             *          1. 根据资源名获取断路器列表, 如果 该资源的断路器列表是空的, 则不作处理
             *          2. 根据资源名获取断路器列表, 如果 该资源的断路器列表是不是空的
             *              遍历断路器执行onRequestComplete方法
             *                  目的就是 计算一些数据
             *                      并且 维护断路器的状态(CLOSE -> OPEN, HALF_OPEN -> CLOSE, HALF_OPEN -> OPEN)
             *
             * @see DegradeSlot
             * @see DegradeRuleManager 降级规则管理器
             *
             */
            chain.entry(context, resourceWrapper, null, count, prioritized, args);
        } catch (BlockException e1) {
            e.exit(count, args);
            throw e1;
        } catch (Throwable e1) {
            // This should not happen, unless there are errors existing in Sentinel internal.
            RecordLog.info("Sentinel unexpected exception", e1);
        }
        return e;
    }

    /**
     * Do all {@link Rule}s checking about the resource.
     *
     * <p>Each distinct resource will use a {@link ProcessorSlot} to do rules checking. Same resource will use
     * same {@link ProcessorSlot} globally. </p>
     *
     * <p>Note that total {@link ProcessorSlot} count must not exceed {@link Constants#MAX_SLOT_CHAIN_SIZE},
     * otherwise no rules checking will do. In this condition, all requests will pass directly, with no checking
     * or exception.</p>
     *
     * @param resourceWrapper resource name
     * @param count           tokens needed
     * @param args            arguments of user method call
     * @return {@link Entry} represents this call
     * @throws BlockException if any rule's threshold is exceeded
     */
    public Entry entry(ResourceWrapper resourceWrapper, int count, Object... args) throws BlockException {
        return entryWithPriority(resourceWrapper, count, false, args);
    }

    /**
     * Get {@link ProcessorSlotChain} of the resource. new {@link ProcessorSlotChain} will
     * be created if the resource doesn't relate one.
     *
     * <p>Same resource({@link ResourceWrapper#equals(Object)}) will share the same
     * {@link ProcessorSlotChain} globally, no matter in which {@link Context}.<p/>
     *
     * <p>
     * Note that total {@link ProcessorSlot} count must not exceed {@link Constants#MAX_SLOT_CHAIN_SIZE},
     * otherwise null will return.
     * </p>
     *
     * @param resourceWrapper target resource
     * @return {@link ProcessorSlotChain} of the resource
     */
    ProcessorSlot<Object> lookProcessChain(ResourceWrapper resourceWrapper) {
        ProcessorSlotChain chain = chainMap.get(resourceWrapper);
        if (chain == null) {
            synchronized (LOCK) {
                chain = chainMap.get(resourceWrapper);
                if (chain == null) {
                    // Entry size limit.
                    if (chainMap.size() >= Constants.MAX_SLOT_CHAIN_SIZE) {
                        return null;
                    }

                    chain = SlotChainProvider.newSlotChain();
                    Map<ResourceWrapper, ProcessorSlotChain> newMap = new HashMap<ResourceWrapper, ProcessorSlotChain>(
                        chainMap.size() + 1);
                    newMap.putAll(chainMap);
                    newMap.put(resourceWrapper, chain);
                    chainMap = newMap;
                }
            }
        }
        return chain;
    }

    /**
     * Get current size of created slot chains.
     *
     * @return size of created slot chains
     * @since 0.2.0
     */
    public static int entrySize() {
        return chainMap.size();
    }

    /**
     * Reset the slot chain map. Only for internal test.
     *
     * @since 0.2.0
     */
    static void resetChainMap() {
        chainMap.clear();
    }

    /**
     * Only for internal test.
     *
     * @since 0.2.0
     */
    static Map<ResourceWrapper, ProcessorSlotChain> getChainMap() {
        return chainMap;
    }

    /**
     * This class is used for skip context name checking.
     */
    private final static class InternalContextUtil extends ContextUtil {
        static Context internalEnter(String name) {
            return trueEnter(name, "");
        }

        static Context internalEnter(String name, String origin) {
            return trueEnter(name, origin);
        }
    }

    @Override
    public Entry entry(String name) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, EntryType.OUT);
        return entry(resource, 1, OBJECTS0);
    }

    @Override
    public Entry entry(Method method) throws BlockException {
        MethodResourceWrapper resource = new MethodResourceWrapper(method, EntryType.OUT);
        return entry(resource, 1, OBJECTS0);
    }

    @Override
    public Entry entry(Method method, EntryType type) throws BlockException {
        MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
        return entry(resource, 1, OBJECTS0);
    }

    @Override
    public Entry entry(String name, EntryType type) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return entry(resource, 1, OBJECTS0);
    }

    @Override
    public Entry entry(Method method, EntryType type, int count) throws BlockException {
        MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
        return entry(resource, count, OBJECTS0);
    }

    @Override
    public Entry entry(String name, EntryType type, int count) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return entry(resource, count, OBJECTS0);
    }

    @Override
    public Entry entry(Method method, int count) throws BlockException {
        MethodResourceWrapper resource = new MethodResourceWrapper(method, EntryType.OUT);
        return entry(resource, count, OBJECTS0);
    }

    @Override
    public Entry entry(String name, int count) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, EntryType.OUT);
        return entry(resource, count, OBJECTS0);
    }

    @Override
    public Entry entry(Method method, EntryType type, int count, Object... args) throws BlockException {
        MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
        return entry(resource, count, args);
    }

    @Override
    public Entry entry(String name, EntryType type, int count, Object... args) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return entry(resource, count, args);
    }

    @Override
    public AsyncEntry asyncEntry(String name, EntryType type, int count, Object... args) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return asyncEntryInternal(resource, count, args);
    }

    @Override
    public Entry entryWithPriority(String name, EntryType type, int count, boolean prioritized) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return entryWithPriority(resource, count, prioritized);
    }

    @Override
    public Entry entryWithPriority(String name, EntryType type, int count, boolean prioritized, Object... args)
        throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, type);
        return entryWithPriority(resource, count, prioritized, args);
    }

    @Override
    public Entry entryWithType(String name, int resourceType, EntryType entryType, int count, Object[] args)
        throws BlockException {
        return entryWithType(name, resourceType, entryType, count, false, args);
    }

    @Override
    public Entry entryWithType(String name, int resourceType, EntryType entryType, int count, boolean prioritized,
                               Object[] args) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, entryType, resourceType);
        return entryWithPriority(resource, count, prioritized, args);
    }

    @Override
    public AsyncEntry asyncEntryWithType(String name, int resourceType, EntryType entryType, int count,
                                         boolean prioritized, Object[] args) throws BlockException {
        StringResourceWrapper resource = new StringResourceWrapper(name, entryType, resourceType);
        return asyncEntryWithPriorityInternal(resource, count, prioritized, args);
    }
}
