package org.sagebionetworks.repo.web.controller.advice;

import org.sagebionetworks.repo.model.AccessRecordExtractorUtil;
import org.sagebionetworks.repo.web.AccessRecordDataListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Optional;

/**
 * Allows to customize/read the response after the execution of an @ResponseBody or a ResponseEntity controller method
 * but before the body is written with an HttpMessageConverter.
 *
 * @author Sandhra
 */
@ControllerAdvice
public class ResponseBodyControllerAdvice implements ResponseBodyAdvice {
    public static final String ID = "id";
    @Autowired
    AccessRecordDataListener listener;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // returning ture as the beforeBodyWrite method should be invoked
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // reading returned object id tof the response body and returning the same response body
        Optional<String> id = AccessRecordExtractorUtil.getObjectFieldValue(body, ID);
        if (id.isPresent()) {
            listener.setReturnObjectId(id.get());
        }
        return body;
    }
}
