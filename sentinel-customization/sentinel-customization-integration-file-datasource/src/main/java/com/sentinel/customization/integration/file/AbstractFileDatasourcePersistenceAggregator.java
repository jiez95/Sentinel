package com.sentinel.customization.integration.file;

import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileWritableDataSource;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * @author : jiez
 * @date : 2021/5/15 17:08
 */
public abstract class AbstractFileDatasourcePersistenceAggregator<T> {

    void persistence() {
        String fileName = getFileName();
        if (StringUtil.isBlank(fileName)) {
            return;
        }
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("文件创建异常");
                // 初始化文件失败不执行持久化
                return;
            }
        }
        registerToWritableDataSourceRegistry(new FileWritableDataSource<>(file, getConfigEncoder(), getCharset()));
    }

    public abstract String getFileName();

    public abstract Converter getConfigEncoder();

    public abstract Charset getCharset();

    /**
     * WritableDataSourceRegistry.registerSystemDataSource();
     * @param fileWritableDataSource
     */
    public abstract void registerToWritableDataSourceRegistry(FileWritableDataSource fileWritableDataSource);

}
