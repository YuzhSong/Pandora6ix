package com.pandora6ix.backend.api;

import static com.pandora6ix.backend.repository.BusinessDb.fields;

import com.pandora6ix.backend.api.error.RequestIdFilter;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.pandora6ix.backend.controller")
public class BusinessResponses implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(
            MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return method.getContainingClass()
                        .getPackageName()
                        .equals("com.pandora6ix.backend.controller")
                && method.getContainingClass()
                        != com.pandora6ix.backend.controller.HealthController.class;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter method,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converter,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body == null) return null;
        var servlet = ((ServletServerHttpRequest) request).getServletRequest();
        response.getHeaders().setCacheControl("no-store");
        return fields(
                "data",
                body,
                "requestId",
                String.valueOf(servlet.getAttribute(RequestIdFilter.ATTRIBUTE_NAME)));
    }
}
