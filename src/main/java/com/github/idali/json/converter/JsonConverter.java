/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.idali.json.converter;
 
import java.util.List; 
import java.util.Map;
import javax.json.JsonObject; 

/**
 *
 * @author idali 
 * @param <T> 
 */
public interface JsonConverter<T extends Object> { 
    
    /**
     * To convert bean/List of beans into json object 
     * 
     * @param bean          - Bean or List of beans
     * @param map           - Key, value map to build meta data in json response
     * @param statusCode    - Status code
     * @return              - JsonObject
     */
    JsonObject convert(T bean, Map<String, ?> map, int statusCode);
    
    /**
     * To build error messages into json object
     * 
     * @param meta          - key, value map to build meta data in json response
     * @param errorMsgs     - A list of string
     * @param statusCode    - Status code
     * @param errorType     - Type of the error
     * @param source        - The source which cause the errors
     * @return 
     */
    JsonObject convert(Map<String, ?> meta, List<String> errorMsgs, int statusCode, String errorType, String source); 
}
