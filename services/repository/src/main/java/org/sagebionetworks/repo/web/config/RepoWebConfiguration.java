package org.sagebionetworks.repo.web.config;

import java.util.Collections;

import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@Configuration
public class RepoWebConfiguration implements WebMvcConfigurer {
	
	@Autowired
	private ObjectTypeSerializer exceptionSerializer;
		
	// Override the default ExceptionHandlerExceptionResolver used by spring so that we can customize the message converters and the content negotiation
	@Bean
	public ExceptionHandlerExceptionResolver controllerExceptionHandlerResolver() {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		
		resolver.setMessageConverters(Collections.singletonList(exceptionSerializer));
		resolver.setContentNegotiationManager(exceptionContentNegotiationManager());
		
		return resolver;
		
	}
	
	// The following beans are not exposed as they are used in place here
	
	private ContentNegotiationManager exceptionContentNegotiationManager() {
		return new ContentNegotiationManager(exceptionContentNegotiationStrategy());
	}
		
	private ContentNegotiationStrategy exceptionContentNegotiationStrategy() {
		return new ExceptionContentNegotiationStrategy(exceptionSerializer.getSupportedMediaTypes());
	}
	
	
}
