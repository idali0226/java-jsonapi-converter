# java-jsonapi-converter

This is a library that provides you a simple way to convert Java Object/List of Java Objects to JsonObject using JSON API standard.

# Overview

The java-jsonapi-converter has two methods:

	- Convert a Java object to JsonObject
		This method takes a Java object (it can be a single object or a collection of object), a Map<String, ?> to build meta data section, and an Integer of status code.

	- Convert error messages to JsonObject
		This method takes a Map<String, ?> to build meta data section, and an Integer of status code, a String to identify error type and a String of the source to cause of the error.

	A package of annotation defined a list of annotations:

		- JsonAPIResource 	(This annotation is on Entity level to define the type of the entity)
		- JsonAPIField		(Optional if the field name needs to be different in JsonObject)
		- JsonAPIId  		(This annotation is on ID field)
		- JsonAPIIgnor 		(Optional if the field needs to excluded from the JsonObject)
		- JsonAPIManyToOne	(Optional to identify a Many to One relationship)
		- JsonAPIOneToMany	(Optional to identify a One to Many relatonship)

	 
# Usage

	With maven project, include this dependency in the pom:

        <dependency>
            <groupId>com.github.idali</groupId>
            <artifactId>java-jsonapi-converter</artifactId>
            <version>0.1-SNAPSHOT</version>
            <type>jar</type>
        </dependency>

    Annotate entities.

    Call JsonConverter and send in the entity / entities to convert into JsonObject

    Example:

        JsonConverter converter = new JsonConverterImpl();

        EntityBean bean = new EntityBean();

        Map<String, Object> meta = new HashMap<>();
        meta.put("call_endpoint", "https://server/api/v1/entityName?offset=0&limit=50&sort=ASC");
        meta.put("result_languages", "en-US");
        meta.put("limit", "50");
        meta.put("api_version", "1");
        ...
 		
        Call method:
        converter.convert(bean, meta, 200);
        or

        List<EntityBean> beans = new ArrayList<>();
        converter.convert(beans, meta, 200);

# Documentation

- [Json API reference](http://jsonapi.org/)

	


