/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.idali.json.converter.util;

import java.sql.Date;
import java.text.SimpleDateFormat; 

/**
 *
 * @author idali
 */
public class Util {
    
    private static Util instance = null;
 
    private final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    public static synchronized Util getInstance() {
        if (instance == null) {
            instance = new Util();
        }
        return instance;
    }
    
    public String dateToString(Date date) {
        if (date != null) {
            return SIMPLE_DATE_FORMAT.format(date);
        } else {
            return null;
        }
    }

    public String dateToString(java.util.Date date) {
        if (date != null) {
            return SIMPLE_DATE_FORMAT.format(date);
        } else {
            return null;
        }
    } 
}
