package cn.lwydyby.openapi.annotation;

import cn.lwydyby.openapi.scanner.scanner.ClassScanner;
import cn.lwydyby.openapi.scanner.scanner.impl.DefaultClassScanner;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author liwei
 * @title: OpenApiSpecGenerator
 * @date 2019-12-18 09:26
 */
public final class OpenApiSpecGenerator {
    private static final Logger log = LoggerFactory.getLogger(OpenApiSpecGenerator.class);

    public static OpenAPI generateOpenApiSpecFromRouter(Router router, String title, String version, String serverUrl, List<String> scanPkgs) {
        log.info("Generating Spec for vertx routes.");
        OpenAPI openAPI = new OpenAPI();
        Info info = new Info();
        info.setTitle(title);
        info.setVersion(version);
        Server server = new Server();
        server.setUrl(serverUrl);
        openAPI.servers(Collections.singletonList(server));
        openAPI.setInfo(info);

        Map<String, PathItem> paths = extractAllPaths(router);
        extractOperationInfo(router, paths, scanPkgs);
        paths.forEach(openAPI::path);
        return openAPI;
    }

    static private Map<String, PathItem> extractAllPaths(Router router) {
        return router.getRoutes().stream().filter(x -> x.getPath() != null)
                .map(Route::getPath).distinct().collect(Collectors.toMap(x -> x, x -> new PathItem()));
    }

    static private void extractOperationInfo(Router router, Map<String, PathItem> paths, List<String> scanPkgs) {
        router.getRoutes().forEach(route -> {
            PathItem pathItem = paths.get(route.getPath());
            if (pathItem != null) {
                List<Operation> operations = extractOperations(route, pathItem);
                operations.forEach(operation -> operation.setParameters(extractPathParams(route.getPath())));
            }
        });
        decorateOperationsFromAnnotationsOnHandlers(paths, scanPkgs);
    }

    private static void decorateOperationsFromAnnotationsOnHandlers(Map<String, PathItem> paths, List<String> scanPkgs) {
        ClassScanner classScanner = new DefaultClassScanner();
        List<Class> classesList = classScanner.scanByAnno(scanPkgs, VertxHandler.class);
        classesList.forEach(cls -> {
            Arrays.stream(cls.getDeclaredMethods()).distinct().forEach(method -> {
                io.swagger.v3.oas.annotations.Operation annotation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
                VertxPath path=method.getAnnotation(VertxPath.class);
                if (annotation != null&&path!=null) {
                    String httpMethod = annotation.method();
                    PathItem pathItem = paths.get(path.path());
                    Operation matchedOperation = null;
                    switch (PathItem.HttpMethod.valueOf(httpMethod.toUpperCase())) {
                        case TRACE:
                            matchedOperation = pathItem.getTrace();
                            break;
                        case PUT:
                            matchedOperation = pathItem.getPut();
                            break;
                        case POST:
                            matchedOperation = pathItem.getPost();
                            break;
                        case PATCH:
                            matchedOperation = pathItem.getPatch();
                            break;
                        case GET:
                            matchedOperation = pathItem.getGet();
                            break;
                        case OPTIONS:
                            matchedOperation = pathItem.getOptions();
                            break;
                        case HEAD:
                            matchedOperation = pathItem.getHead();
                            break;
                        case DELETE:
                            matchedOperation = pathItem.getDelete();
                            break;
                        default:
                            break;
                    }
                    if (matchedOperation != null) {
                        AnnotationMappers.decorateOperationFromAnnotation(annotation, matchedOperation);
                        RequestBody body = method.getParameters()[0].getAnnotation(RequestBody.class);
                        if (body != null) {
                            matchedOperation.setRequestBody(AnnotationMappers.fromRequestBody(body));
                        }
                    }
                }
            });

        });
    }

    private static List<Parameter> extractPathParams(String fullPath) {
        String[] split = fullPath.split("\\/");
        return Arrays.stream(split).filter(x -> x.startsWith(":")).map(x -> {
            Parameter param = new Parameter();
            param.name(x.substring(1));
            return param;
        }).collect(Collectors.toList());
    }

    private static List<Operation> extractOperations(Route route, PathItem pathItem) {
        Set<HttpMethod> httpMethods = route.methods();
        return httpMethods.stream().map(httpMethod -> {
            Operation operation = new Operation();
            switch (PathItem.HttpMethod.valueOf(httpMethod.name())) {
                case TRACE:
                    pathItem.trace(operation);
                    break;
                case PUT:
                    pathItem.put(operation);
                    break;
                case POST:
                    pathItem.post(operation);
                    break;
                case PATCH:
                    pathItem.patch(operation);
                    break;
                case GET:
                    pathItem.get(operation);
                    break;
                case OPTIONS:
                    pathItem.options(operation);
                    break;
                case HEAD:
                    pathItem.head(operation);
                    break;
                case DELETE:
                    pathItem.delete(operation);
                    break;
                default:
                    break;
            }
            return operation;
        }).collect(Collectors.toList());


    }
}
