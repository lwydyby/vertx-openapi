package cn.lwydyby.openapi.scanner.scanner;

import cn.lwydyby.openapi.scanner.callback.ScannerCallback;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * @author: liwei
 * @description:
 */
public interface ClassScanner {

    /**
     * 扫描多个包下的Class
     *
     * @param scanBasePackages
     * @return
     */
    List<Class> scan(List<String> scanBasePackages);

    /**
     * 扫描多个包下带有注解的Class
     *
     * @param scanBasePackages
     * @param anno
     * @return
     */
    List<Class> scanByAnno(List<String> scanBasePackages, Class<? extends Annotation> anno);

    /**
     * 扫描多个包下的Class，并执行回调
     *
     * @param scanBasePackages
     * @param callback
     */
    void scanAndCallback(List<String> scanBasePackages, ScannerCallback callback);

    /**
     * 扫描多个包下特定注解的Class，并执行回调
     *
     * @param scanBasePackages
     * @param anno
     * @param callback
     */
    void scanAndCallbackByAnno(List<String> scanBasePackages, Class<? extends Annotation> anno, ScannerCallback callback);
}
