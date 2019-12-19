package cn.lwydyby.openapi.annotation;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liwei
 * @title: AnnotationMappers
 * @date 2019-12-18 09:33
 */
public final class AnnotationMappers {

    private static final Logger log = LoggerFactory.getLogger(AnnotationMappers.class);

    static void decorateOperationFromAnnotation(Operation annotation, io.swagger.v3.oas.models.Operation operation) {
        operation.summary(annotation.summary());
        operation.description(annotation.description());
        operation.operationId(annotation.operationId());
        operation.deprecated(annotation.deprecated());

        ApiResponses apiResponses = new ApiResponses();
        apiResponses.putAll(
                Arrays.stream(annotation.responses()).map(response -> {
                    ApiResponse apiResponse = new ApiResponse();
                    apiResponse.description(response.description());
                    if (response.content().length > 0) {
                        Arrays.stream(response.content()).forEach(content -> {
                            Content c = getContent(content);
                            if (!Void.class.equals(content.array().schema().implementation()))
                                c.get(content.mediaType()).getSchema().setExample(clean(content.array().schema().example()));
                            else if (!Void.class.equals(content.schema().implementation()))
                                c.get(content.mediaType()).getSchema().setExample(content.schema().example());
                            apiResponse.content(c);
                        });
                    }
                    Arrays.stream(response.headers()).forEach(header -> {
                        Header h = new Header();
                        h.description(header.description());
                        h.deprecated(header.deprecated());
                        Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(header.schema(),null);
                        schemaFromAnnotation.ifPresent(h::schema);
                        h.required(header.required());
                        apiResponse.addHeaderObject(header.name(), h);
                    });
                    return new ImmutablePair<>(response.responseCode(), apiResponse);
                }).collect(Collectors.toMap(x -> x.left, x -> x.right)));
        operation.responses(apiResponses);
        Arrays.stream(annotation.parameters()).forEach(parameter -> {
            Parameter p = findAlreadyProcessedParamFromVertxRoute(parameter.name(), operation.getParameters());
            if (p == null) {
                p = new Parameter();
                operation.addParametersItem(p);
            }
            p.name(parameter.name());
            p.description(parameter.description());
            p.allowEmptyValue(parameter.allowEmptyValue());
            try {
                p.style(Parameter.StyleEnum.valueOf(parameter.style().name()));
            } catch (IllegalArgumentException ie) {
                log.warn(ie.getMessage());
            }
            p.setRequired(parameter.required());
            p.in(parameter.in().name().toLowerCase());
            Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(parameter.schema(),null);
            schemaFromAnnotation.ifPresent(p::schema);
        });
    }

    private static Object clean(final String in) {
        return in;
    }

    private static Content getContent(io.swagger.v3.oas.annotations.media.Content content) {
        Content c = new Content();
        MediaType mediaType = new MediaType();
        Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(content.schema(),null);
        schemaFromAnnotation.ifPresent(mediaType::setSchema);
        if (!schemaFromAnnotation.isPresent()) {
            Optional<ArraySchema> arraySchema = AnnotationsUtils.getArraySchema(content.array(),null);
            arraySchema.ifPresent(mediaType::setSchema);
        }
        c.addMediaType(content.mediaType(), mediaType);
        return c;
    }

    private static Parameter findAlreadyProcessedParamFromVertxRoute(final String name, List<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (name.equals(parameter.getName()))
                return parameter;
        }
        return null;
    }

    static io.swagger.v3.oas.models.parameters.RequestBody fromRequestBody(RequestBody body, Components components) {
        io.swagger.v3.oas.models.parameters.RequestBody rb = new io.swagger.v3.oas.models.parameters.RequestBody();
        rb.setDescription(body.description());
        if (body.content().length == 1) {
            Content c = getContent(body.content()[0]);
            io.swagger.v3.oas.annotations.media.Content content = body.content()[0];
            if (!Void.class.equals(content.array().schema().implementation()))
                c.get(content.mediaType()).getSchema().setExample(clean(content.array().schema().example()));
            else if (!Void.class.equals(content.schema().implementation())) {
                Class schemaClass=content.schema().implementation();
                String schemaKey=AnnotationsUtils.COMPONENTS_REF+schemaClass.getSimpleName();
                if(components.getSchemas()==null){
                    components.setSchemas(new HashMap<>());
                }
                if(!components.getSchemas().containsKey(schemaClass.getSimpleName())){
                    Schema schema=getSchema(schemaClass,components);
                    components.getSchemas().put(schemaClass.getSimpleName(),schema);
                }
                c.get(content.mediaType()).getSchema().set$ref(schemaKey);
            }
            rb.setContent(c);
        }
        return rb;
    }
    private static Schema getSchema(Class schemaClass,Components components){
        Field[] fields=schemaClass.getDeclaredFields();
        Schema schema=new Schema();
        Map<String,Schema> properties=new HashMap<>();
        for(Field field:fields){
            io.swagger.v3.oas.annotations.media.Schema annotation=field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
            if(annotation==null){
                continue;
            }
            Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(annotation,null);
            schemaFromAnnotation.ifPresent(schema1 -> {
                String type=transType(field.getType());
                if(type.equals("object")){
                    if(!components.getSchemas().containsKey(field.getType().getSimpleName())){
                        components.getSchemas().put(field.getType().getSimpleName(),getSchema(field.getType(),components));
                    }
                    schema1.set$ref(AnnotationsUtils.COMPONENTS_REF+field.getType().getSimpleName());
                }
                schema1.setType(type);
                properties.put(field.getName(),schema1);
            });
        }
        schema.setName(schemaClass.getSimpleName());
        schema.setProperties(properties);
        schema.setType("object");
        return schema;
    }

    private static String transType(Class type){
        if(type.isAssignableFrom(Integer.class)||type.isAssignableFrom(Long.class)
                ||type.isAssignableFrom(int.class) ||type.isAssignableFrom(long.class)){
            return "integer";
        }else if(type.isAssignableFrom(Double.class)||type.isAssignableFrom(Float.class)
                ||type.isAssignableFrom(double.class) ||type.isAssignableFrom(float.class)){
            return "number";
        }else if(type.isAssignableFrom(String.class)||type.isAssignableFrom(Byte.class)
                ||type.isAssignableFrom(Date.class) ||type.isAssignableFrom(byte.class)||type.isAssignableFrom(Enum.class)){
            return "string";
        }else if(type.isAssignableFrom(Boolean.class)||type.isAssignableFrom(boolean.class)){
            return "boolean";
        }else if(type.isEnum()){
            return "string";
        }else if(type.isArray()||type.isAssignableFrom(List.class)||type.isAssignableFrom(Set.class)){
            return "array";
        }
        return "object";
    }


}
