package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.spi.SpiLoader;
import com.sentinel.customization.integration.file.AbstractFileDatasourcePersistenceAggregator;

import java.util.List;

/**
 * @author : jiez
 * @date : 2021/5/15 16:44
 */
public class FileDataSourceInitFunc implements InitFunc {

    @Override
    public void init() throws Exception {
        System.out.println("开始初始化【文件持久化数据源】");
        List<AbstractFileDatasourcePersistenceAggregator> fileDatasourcePersistenceAggregators =
                SpiLoader.of(AbstractFileDatasourcePersistenceAggregator.class).loadInstanceList();

        fileDatasourcePersistenceAggregators.forEach(datasource -> datasource.persistence());
        System.out.println("【文件持久化数据源】初始化结束");
    }


}
