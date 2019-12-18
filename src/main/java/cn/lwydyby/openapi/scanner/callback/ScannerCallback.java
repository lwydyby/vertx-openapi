package cn.lwydyby.openapi.scanner.callback;

import java.util.List;

/**
 * @author: liwei
 * @description: Callback函数接口
 */
public interface ScannerCallback {

    /**
     * 回调方法
     *
     * @param clzs
     */
    void callback(List<Class> clzs);
}
