/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.idali.json.converter.util;
 

/**
 *
 * @author idali
 */
public class CommonString {
     
    private final String DATA = "data";
    private final String META = "meta";
    private final String TYPE = "type";
    private final String ID = "id";
    private final String COUNT = "count";
    private final String POINT = "point";
    private final String DETAIL = "detail";
    private final String SOURCE = "source";
    private final String ERRORS = "errors";
    private final String ATTRIBUTES = "attributes";
    private final String RELATIONSHIP = "relationships";
    private final String STATUS_CODE = "status_code";
    private final String RESULT_COUNT = "result_count";
    
    private static CommonString instance = null; 

    public static synchronized CommonString getInstance() {
        if (instance == null) {
            instance = new CommonString();
        }
        return instance;
    }
    
    public String getStatusCode() {
        return STATUS_CODE;
    }
    
    public String getResultCount() {
        return RESULT_COUNT;
    }
    
    public String getRelationship() {
        return RELATIONSHIP;
    }
    
    public String getAttributes() {
        return ATTRIBUTES;
    }
    
    public String getData() {
        return DATA;
    }
    
    public String getMeta() {
        return META;
    }
    
    public String getType() {
        return TYPE;
    }
    
    public String getId() {
        return ID;
    }
    
    public String getCount() {
        return COUNT;
    }
    
    public String getDetail() {
        return DETAIL;
    }
    
    public String getSource() {
        return SOURCE;
    }
    
    public String getPoint() {
        return POINT;
    }
    
    public String getErrors() {
        return ERRORS;
    }
}
