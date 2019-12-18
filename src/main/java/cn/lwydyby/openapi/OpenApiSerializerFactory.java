package cn.lwydyby.openapi;

import cn.lwydyby.openapi.annotation.OpenApiSpecGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.ext.web.Router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liwei
 * @title: OpenApiSerializerFactory
 * @description: 用来选择加载openApi对象参数的方式  Annotation/database
 * @date 2019-12-17 10:07
 */

public class OpenApiSerializerFactory {

    private final static Map<String, OpenAPI> generatedSpecs = new HashMap<>();

    public static OpenAPI  getOpenApiByAnnotation(Router router, String title, String version, String serverUrl, List<String> scanPkgs){
        return OpenApiSpecGenerator.generateOpenApiSpecFromRouter(router, title, version, serverUrl,scanPkgs);
    }
    //TODO
    public static OpenAPI getOpenApiByDataBase(String title, String version, String serverUrl){
        return null;
    }


}