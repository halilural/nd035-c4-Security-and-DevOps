package com.example.demo;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

public class RequestBuilder {
    private Map<String, String> body;
    private HttpHeaders headers;

    public RequestBuilder() {
        this.body = new HashMap<>();
        this.headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    RequestBuilder addBodyProperty(String key, String value){
        this.body.put(key, value);
        return this;
    }

    RequestBuilder setAuthToken(String token){
        headers.add("Authorization", token);
        return this;
    }

    HttpEntity build(){
        HttpEntity entity = new HttpEntity(body, headers);
        return entity;
    }
}
