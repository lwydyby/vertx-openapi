package cn.lwydyby.openapi.scanner.scanner.impl;

import cn.lwydyby.openapi.scanner.callback.ScannerCallback;
import cn.lwydyby.openapi.scanner.scanner.ClassScanner;
import cn.lwydyby.openapi.scanner.util.ClassUtil;
import cn.lwydyby.openapi.scanner.util.EmptyUtil;
import cn.lwydyby.openapi.scanner.util.ExecutorUtil;
import cn.lwydyby.openapi.scanner.util.ScannerUtil;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.AllArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @author: liwei
 * @description: 默认类扫描器实现类
 */

public class DefaultClassScanner implements ClassScanner {

    Logger log=LoggerFactory.getLogger(DefaultClassScanner.class);

    @Override
    public List<Class> scan(List<String> scanBasePackages) {
        List<Class> classList = new LinkedList<>();

        //没有需要扫描的包，返回空列表
        if (EmptyUtil.isEmpty(scanBasePackages)) {
            return classList;
        }

        //去除重复包
        List<String> realScanBasePackages = ClassUtil.distinctPackages(scanBasePackages);

        //创建异步线程
        List<FutureTask<List<Class>>> tasks = new LinkedList<>();
        realScanBasePackages
                .forEach(pkg -> {
                    ScannerCallable call = new ScannerCallable(pkg);
                    FutureTask<List<Class>> task = new FutureTask(call);
                    ExecutorUtil.executeInPool(new Thread(task));
                    tasks.add(task);
                });

        //等待返回结果
        tasks.parallelStream().forEach(task -> {
            try {
                classList.addAll(task.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        });


        return classList;
    }

    /**
     * @param scanBasePackages
     * @param anno
     * @return
     */
    @Override
    public List<Class> scanByAnno(List<String> scanBasePackages, Class<? extends Annotation> anno) {
        List<Class> classList = this.scan(scanBasePackages);

        //根据Annotation过滤并返回
        return classList.parallelStream()
                .filter(clz -> {
                    try {
                        if (clz.getAnnotation(anno) == null) {
                            return false;
                        }
                    } catch (Throwable e) {
                        log.debug(e.getMessage());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void scanAndCallback(List<String> scanBasePackages, ScannerCallback callback) {
        List<Class> classList = this.scan(scanBasePackages);
        callback.callback(classList);
    }

    @Override
    public void scanAndCallbackByAnno(List<String> scanBasePackages,
                                      Class<? extends Annotation> anno,
                                      ScannerCallback callback) {
        List<Class> classList = this.scanByAnno(scanBasePackages, anno);
        callback.callback(classList);
    }

    /**
     * 扫描器线程类
     */
    @AllArgsConstructor
    class ScannerCallable implements Callable<List<Class>> {

        /**
         * 扫描的包名称
         */
        private String pkg;

        @Override
        public List<Class> call() {
            return ScannerUtil.scan(pkg);
        }
    }
}
