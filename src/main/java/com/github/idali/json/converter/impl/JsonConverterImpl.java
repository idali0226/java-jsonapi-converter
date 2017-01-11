/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.idali.json.converter.impl;

import com.github.idali.json.converter.JsonConverter;
import com.github.idali.json.converter.annotation.JsonAPIId;
import com.github.idali.json.converter.annotation.JsonAPIIgnor;
import com.github.idali.json.converter.annotation.JsonAPIRelationship;
import com.github.idali.json.converter.util.Util;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays; 
import java.util.Collection;
import java.util.List;
import java.util.Map; 
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author idali
 * @param <T>
 */
public class JsonConverterImpl<T extends Object> implements JsonConverter<T>, Serializable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private final String DATATYPE_LIST = "java.util.List";
    private final String DATATYPE_COLLECTION = "java.util.Collection";
    private final String DATATYPE_SET = "java.util.Set";

    @Override
    public JsonObject convert(T object, Map<String, ?> map, int status) {
  
        if (object instanceof List) {
            List<Object> beans = (List<Object>) object;
            return convertList(beans, map, status);
        } else {
            return convertBean(object, map, status);
        }
    }

    @Override
    public JsonObject convert(int count, Map<String, ?> meta, String entityName) {

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = factory.createObjectBuilder();
        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
        dataBuilder.add("type", entityName);
        dataBuilder.add("count", count);

        jsonBuilder.add("meta", buildMetadata(meta, 1, 200));
        jsonBuilder.add("data", dataBuilder);
        return jsonBuilder.build();
    }

    @Override
    public JsonObject convert(Map<String, ?> meta, List<String> errorMsgs,
            int status, String errorType, String source) {

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = factory.createObjectBuilder();

        jsonBuilder.add("meta", buildMetadata(meta, 0, status));

        JsonObjectBuilder sourceBuilder = Json.createObjectBuilder();
        sourceBuilder.add("point", source);
        JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
        errorMsgs.stream()
                .forEach(l -> {
                    JsonObjectBuilder errorBuilder = Json.createObjectBuilder();
                    errorBuilder.add("detail", l);
                    errorBuilder.add("source", sourceBuilder);
                    arrBuilder.add(errorBuilder);
                });

        jsonBuilder.add("errors", arrBuilder);
        return jsonBuilder.build();
    }
      
    private JsonObject convertList(List<Object> beans, Map<String, ?> map, int status) {

        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = factory.createObjectBuilder();
        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
        JsonArrayBuilder dataArrBuilder = Json.createArrayBuilder();

        if (!beans.isEmpty()) {
            String type = beans.get(0).getClass().getSimpleName();
            beans.stream()
                    .forEach(b -> {
                        buildDataBody(b, type, dataBuilder);
                        dataArrBuilder.add(dataBuilder);
                    });
        }

        jsonBuilder.add("meta", buildMetadata(map, beans.size(), status));
        jsonBuilder.add("data", dataArrBuilder);
        return jsonBuilder.build();
    }

    private void buildDataBody(Object object, String type, JsonObjectBuilder dataBuilder) {

        JsonObjectBuilder attBuilder = Json.createObjectBuilder();
        JsonObjectBuilder relBuilder = Json.createObjectBuilder();

        dataBuilder.add("type", type);
        dataBuilder.add("id", getIdField(object));

        Field[] fields = object.getClass().getDeclaredFields();
        Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(JsonAPIIgnor.class))
                .forEach(f -> {
                    Object fieldValue = getFieldValue(f, object);
                    if (fieldValue != null) {
                        if (f.isAnnotationPresent(JsonAPIRelationship.class)) {
                            switch (f.getType().getName()) {
                                case DATATYPE_LIST:
                                    buildOneToManyRelationship(f, fieldValue, relBuilder);
                                    break;
                                case DATATYPE_COLLECTION: 
                                    buildOneToManyRelationship(f, new ArrayList((Collection) fieldValue), relBuilder);
                                    break; 
                                case DATATYPE_SET: 
                                    buildOneToManyRelationship(f, new ArrayList((Set) fieldValue), relBuilder);
                                    break;
                                default:
                                    buildManyToOneRelationship(f, fieldValue, relBuilder);
                                    break;
                            }
                        } else {
                            addAttributes(attBuilder, fieldValue, f.getName());
                        }
                    }
                });

        dataBuilder.add("attributes", attBuilder);
        dataBuilder.add("relationships", relBuilder);
    }

    private JsonObject convertBean(Object object, Map<String, ?> map, int status) {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = factory.createObjectBuilder();
        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

        if(object != null) {
            String type = object.getClass().getSimpleName(); 
            buildDataBody(object, type, dataBuilder);
        } 
          
        jsonBuilder.add("meta", buildMetadata(map, 1, status));
        jsonBuilder.add("data", dataBuilder);
        return jsonBuilder.build();
    }
 
    private void buildOneToManyRelationship(Field field, Object bean, JsonObjectBuilder relBuilder) {

        JsonObjectBuilder subBuilder = Json.createObjectBuilder();
        JsonObjectBuilder subDataBuilder = Json.createObjectBuilder();
        JsonArrayBuilder subDataArrBuilder = Json.createArrayBuilder();

        List<Object> subBeans = (List<Object>) bean;
        if (subBeans != null && !subBeans.isEmpty()) {
//            String fieldName = field.getAnnotation(DinaOneToMany.class).name();
//            String type = field.getAnnotation(DinaOneToMany.class).type(); 
            String type = field.getName();
            subBeans.stream()
                    .forEach(b -> {
                        subDataBuilder.add("type", type);
                        subDataBuilder.add("id", getIdField(b));
                        subDataArrBuilder.add(subDataBuilder);
                    });
            subBuilder.add("data", subDataArrBuilder);
            relBuilder.add(type, subBuilder);
        }
    }

    private void buildManyToOneRelationship(Field field, Object bean, JsonObjectBuilder relBuilder) {
        JsonObjectBuilder subBuilder = Json.createObjectBuilder();
        if (bean != null) {
//            String fieldName = field.getAnnotation(DinaManyToOne.class).name();
//            String type = field.getAnnotation(DinaManyToOne.class).type(); 
            JsonObjectBuilder subDataBuilder = Json.createObjectBuilder();
            subDataBuilder.add("type", field.getName());
            subDataBuilder.add("id", getIdField(bean));
            subBuilder.add("data", subDataBuilder);
            relBuilder.add(field.getName(), subBuilder);
        }
    }

    private void addAttributes(JsonObjectBuilder attBuilder, Object value, String key) {
         
        if (value instanceof Integer) {
            attBuilder.add(key, (int) value);
        } else if (value instanceof Short) {
            attBuilder.add(key, (Short) value);
        } else if (value instanceof Date) {
            attBuilder.add(key, Util.getInstance().dateToString((Date) value));
        } else if (value instanceof java.util.Date) {
            attBuilder.add(key, Util.getInstance().dateToString((java.util.Date) value));
        } else if (value instanceof BigDecimal) {
            attBuilder.add(key, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            attBuilder.add(key, (Boolean) value);
        } else if (value instanceof Double) {
            attBuilder.add(key, (Double) value);
        } else if (value instanceof Float) {
            attBuilder.add(key, (Float) value);
        } else {
            attBuilder.add(key, (String) value);
        }
    }

    private int getIdField(Object bean) {
        try {
            Field field = Arrays.asList(bean.getClass().getDeclaredFields())
                    .stream()
                    .filter(f -> f.isAnnotationPresent(JsonAPIId.class))
                    .findAny()
                    .get();

            field.setAccessible(true);
            return (Integer) field.get(bean);
        } catch (IllegalArgumentException | IllegalAccessException | java.util.NoSuchElementException ex) {
            return 0;
        }
    }

    private JsonObjectBuilder buildMetadata(Map<String, ?> map, int numOfResult, int status) {
        JsonObjectBuilder metaBuilder = Json.createObjectBuilder();
        map.entrySet().stream()
                .forEach(m -> {
                    Object value = m.getValue();
                    if (value instanceof java.util.List) {
                        List<String> list = (List) value;
                        JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
                        list.stream()
                                .forEach(l -> {
                                    arrBuilder.add(l);
                                });
                        metaBuilder.add(m.getKey(), arrBuilder);
                    } else if (value instanceof Integer) {
                        metaBuilder.add(m.getKey(), (int) value);
                    } else {
                        metaBuilder.add(m.getKey(), (String) value);
                    }
                });
        metaBuilder.add("statusCode", status);
        metaBuilder.add("resultCount", numOfResult);
        return metaBuilder;
    }

    private Object getFieldValue(Field field, Object bean) {
        try {
            field.setAccessible(true);
            return field.get(bean);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            logger.error(ex.getMessage());
            return null;
        }
    }
}
