package org.sagebionetworks.repo.web.controller.advice;

import org.sagebionetworks.repo.model.AccessRecordExtractorUtil;
import org.sagebionetworks.repo.web.AccessRecordDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Allows to customize/read the request body before it is passed into a controller method as an @RequestBody or an HttpEntity method argument.
 *
 * @author Sandhra
 */
@ControllerAdvice
public class RequestBodyControllerAdvice implements RequestBodyAdvice {
    public static final String CONCRETE_TYPE = "concreteType";
    @Autowired
    AccessRecordDataListener listener;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        // returning ture as the interceptor should be invoked
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        // returning same input message as nothing to do here
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        // reading concrete type of the request body and returning the same request body
        Optional<String> concreteType = AccessRecordExtractorUtil.getObjectFieldValue(body, CONCRETE_TYPE);
        concreteType.ifPresent(s -> listener.setRequestConcreteType(s));
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        // returning same body as nothing to do here
        return body;
    }
}
