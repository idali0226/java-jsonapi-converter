/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.idali.json.converter.impl;

import com.github.idali.json.converter.JsonConverter;
import com.github.idali.json.converter.annotation.JsonAPIField;
import com.github.idali.json.converter.annotation.JsonAPIId;
import com.github.idali.json.converter.annotation.JsonAPIIgnor;
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
//import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.idali.json.converter.annotation.JsonAPIManyToOne;
import com.github.idali.json.converter.annotation.JsonAPIOneToMany;
import com.github.idali.json.converter.annotation.JsonAPIResource;
import com.github.idali.json.converter.util.CommonString;

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
    public JsonObject convert(T object, Map<String, ?> meta, int status) {
  
        if(object == null) {
            return null;
        }
        
        if (object instanceof List) {                       // If the object is a list of beans, convert list
            List<Object> beans = (List<Object>) object;
            return convertList(beans, meta, status);
        } else {
            return convertBean(object, meta, status);       // Convert single bean
        }
    }

    @Override
    public JsonObject convert(Map<String, ?> meta, List<String> errorMsgs,
                             int status, String errorType, String source) {

//        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder(); 
        
        // Add meta section
        jsonBuilder.add(CommonString.getInstance().getMeta(), buildMetadata(meta, 0, status));

        JsonObjectBuilder sourceBuilder = Json.createObjectBuilder();
        sourceBuilder.add(CommonString.getInstance().getPoint(), source);
        JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
        errorMsgs.stream()
                .forEach(l -> {
                    JsonObjectBuilder errorBuilder = Json.createObjectBuilder();
                    errorBuilder.add(CommonString.getInstance().getDetail(), l);
                    errorBuilder.add(CommonString.getInstance().getSource(), sourceBuilder);
                    arrBuilder.add(errorBuilder);
                });

        jsonBuilder.add(CommonString.getInstance().getErrors(), arrBuilder);
        return jsonBuilder.build();
    }
      
    private JsonObject convertList(List<Object> beans, Map<String, ?> meta, int status) {

//        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();
        JsonArrayBuilder dataArrBuilder = Json.createArrayBuilder();

        if (!beans.isEmpty()) {
            String type = beans.get(0).getClass().getAnnotation(JsonAPIResource.class).type();
            beans.stream()
                    .forEach(b -> {
                        buildDataBody(b, type, dataBuilder);
                        dataArrBuilder.add(dataBuilder);
                    });
        }

        jsonBuilder.add(CommonString.getInstance().getMeta(), buildMetadata(meta, beans.size(), status));
        jsonBuilder.add(CommonString.getInstance().getData(), dataArrBuilder);
        return jsonBuilder.build();
    }
    
    private JsonObject convertBean(Object object, Map<String, ?> meta, int status) {
//        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

        if(object != null) {
            String type = object.getClass().getAnnotation(JsonAPIResource.class).type(); 
            buildDataBody(object, type, dataBuilder);
        } 
          
        jsonBuilder.add(CommonString.getInstance().getMeta(), buildMetadata(meta, 1, status));
        jsonBuilder.add(CommonString.getInstance().getData(), dataBuilder);
        return jsonBuilder.build();
    }

    /**
     * Build data section
     * 
     * @param object
     * @param type
     * @param dataBuilder 
     */
    private void buildDataBody(Object object, String type, JsonObjectBuilder dataBuilder) {

        JsonObjectBuilder attBuilder = Json.createObjectBuilder();
        JsonObjectBuilder relBuilder = Json.createObjectBuilder();

        dataBuilder.add(CommonString.getInstance().getType(), type);
        dataBuilder.add(CommonString.getInstance().getId(), getIdField(object));

        Field[] fields = object.getClass().getDeclaredFields();
        Arrays.stream(fields)
                .filter(field -> !field.isAnnotationPresent(JsonAPIIgnor.class))
                .forEach(f -> {
                    Object fieldValue = getFieldValue(f, object);
                    if (fieldValue != null) {
                        if (f.isAnnotationPresent(JsonAPIManyToOne.class)) {
                            switch (f.getType().getName()) { 
                                case DATATYPE_COLLECTION: 
                                    buildManyToOneRelationship(f, new ArrayList((Collection) fieldValue), relBuilder);
                                    break; 
                                case DATATYPE_SET: 
                                    buildManyToOneRelationship(f, new ArrayList((Set) fieldValue), relBuilder);
                                    break;
                                default:
                                    buildManyToOneRelationship(f, fieldValue, relBuilder);
                                    break;
                            }
                        } else if(f.isAnnotationPresent((JsonAPIOneToMany.class))) {
                            buildOneToManyRelationship(f, fieldValue, relBuilder);
                        } else if(f.isAnnotationPresent(JsonAPIField.class)) {
                            addAttributes(attBuilder, fieldValue, f.getAnnotation(JsonAPIField.class).name());
                        } else {
                            if(!f.isAnnotationPresent(JsonAPIId.class)) {
                                addAttributes(attBuilder, fieldValue, f.getName());
                            } 
                        }
                    }
                }); 
        dataBuilder.add(CommonString.getInstance().getAttributes(), attBuilder);
        dataBuilder.add(CommonString.getInstance().getRelationship(), relBuilder);
    }
 
    private void buildOneToManyRelationship(Field field, Object bean, JsonObjectBuilder relBuilder) {

        JsonObjectBuilder subBuilder = Json.createObjectBuilder();
        JsonObjectBuilder subDataBuilder = Json.createObjectBuilder();
        JsonArrayBuilder subDataArrBuilder = Json.createArrayBuilder();

        List<Object> subBeans = (List<Object>) bean;
        if (subBeans != null && !subBeans.isEmpty()) {
            String fieldName = field.getAnnotation(JsonAPIOneToMany.class).name();
            String type = field.getAnnotation(JsonAPIOneToMany.class).type(); 
            subBeans.stream()
                    .forEach(b -> {
                        subDataBuilder.add(CommonString.getInstance().getType(), type);
                        subDataBuilder.add(CommonString.getInstance().getId(), getIdField(b));
                        subDataArrBuilder.add(subDataBuilder);
                    });
            subBuilder.add(CommonString.getInstance().getData(), subDataArrBuilder);
            relBuilder.add(fieldName, subBuilder);
        }
    }

    private void buildManyToOneRelationship(Field field, Object bean, JsonObjectBuilder relBuilder) {
        JsonObjectBuilder subBuilder = Json.createObjectBuilder();
        if (bean != null) {
            String fieldName = field.getAnnotation(JsonAPIManyToOne.class).name();
            String type = field.getAnnotation(JsonAPIManyToOne.class).type(); 
            JsonObjectBuilder subDataBuilder = Json.createObjectBuilder();
            subDataBuilder.add(CommonString.getInstance().getType(), type);
            subDataBuilder.add(CommonString.getInstance().getId(), getIdField(bean));
            subBuilder.add(CommonString.getInstance().getData(), subDataBuilder);
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
        } else if (value instanceof Long ) {
            attBuilder.add(key, (Long) value);
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

    /**
     * Build metadata.  
     * @param map           - key value map.  Value can be int, String and List
     * @param numOfResult   - number of the result to return
     * @param status        - status code
     * @return              - json object
     */
    private JsonObjectBuilder buildMetadata(Map<String, ?> meta, int numOfResult, int status) {
        JsonObjectBuilder metaBuilder = Json.createObjectBuilder();
        meta.entrySet().stream()
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
        metaBuilder.add(CommonString.getInstance().getStatusCode(), status);
        metaBuilder.add(CommonString.getInstance().getResultCount(), numOfResult);
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
