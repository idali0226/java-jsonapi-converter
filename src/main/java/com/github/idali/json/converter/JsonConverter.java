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
    
    JsonObject convert(T bean, Map<String, ?> map, int statusCode);
     
    JsonObject convert(int count, Map<String, ?>  meta, String source);
    
    JsonObject convert(Map<String, ?> meta, List<String> errorMsgs, int statusCode, String errorType, String source); 
    
}
