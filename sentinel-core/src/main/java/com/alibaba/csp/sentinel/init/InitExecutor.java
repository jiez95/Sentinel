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
package com.alibaba.csp.sentinel.init;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.spi.SpiLoader;

/**
 * Load registered init functions and execute in order.
 *
 * @author Eric Zhao
 */
public final class InitExecutor {

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * If one {@link InitFunc} throws an exception, the init process
     * will immediately be interrupted and the application will exit.
     *
     * The initialization will be executed only once.
     */
    public static void doInit() {
        // 利用CAS，防并发初始化环境
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        try {
            List<InitFunc> initFuncs =
                    SpiLoader
                            /**
                             * 以classFullName作为唯一标识缓存SpiLoader
                             *  优先走缓存
                             *  如果缓存不能命中，则new一个出来并加入缓存
                             */
                            .of(InitFunc.class)
                            /**
                             * 目标路径: /META-INF/services/serviceClassFullName
                             * 利用classloader加载对应文件URL，
                             *      然后根据URL读取文件内从，
                             *      解析文件内容利用Class.forName进行加载
                             *
                             * 可以利用 注解-@Spi 来指定目标加载类的别名
                             *
                             * 每个SpiLoader实例都有一个类缓存，优先用指定别名，其次用类全限定名
                             * 如果重复加载同一个类会报错
                             *
                             * 注解-@Spi 可以指定默认类，但只能指定一次，不然会报错
                             */
                            .loadInstanceListSorted();
            List<OrderWrapper> initList = new ArrayList<OrderWrapper>();
            // 对InitFunc实现类进行排序
            for (InitFunc initFunc : initFuncs) {
                RecordLog.info("[InitExecutor] Found init func: {}", initFunc.getClass().getCanonicalName());
                insertSorted(initList, initFunc);
            }
            // 遍历InitFunc实现类，执行init方法
            for (OrderWrapper w : initList) {
                w.func.init();
                RecordLog.info("[InitExecutor] Executing {} with order {}",
                    w.func.getClass().getCanonicalName(), w.order);
            }
        } catch (Exception ex) {
            RecordLog.warn("[InitExecutor] WARN: Initialization failed", ex);
            ex.printStackTrace();
        } catch (Error error) {
            RecordLog.warn("[InitExecutor] ERROR: Initialization failed with fatal error", error);
            error.printStackTrace();
        }
    }

    private static void insertSorted(List<OrderWrapper> list, InitFunc func) {
        int order = resolveOrder(func);
        int idx = 0;
        for (; idx < list.size(); idx++) {
            if (list.get(idx).getOrder() > order) {
                break;
            }
        }
        list.add(idx, new OrderWrapper(order, func));
    }

    private static int resolveOrder(InitFunc func) {
        if (!func.getClass().isAnnotationPresent(InitOrder.class)) {
            return InitOrder.LOWEST_PRECEDENCE;
        } else {
            return func.getClass().getAnnotation(InitOrder.class).value();
        }
    }

    private InitExecutor() {}

    private static class OrderWrapper {
        private final int order;
        private final InitFunc func;

        OrderWrapper(int order, InitFunc func) {
            this.order = order;
            this.func = func;
        }

        int getOrder() {
            return order;
        }

        InitFunc getFunc() {
            return func;
        }
    }
}
