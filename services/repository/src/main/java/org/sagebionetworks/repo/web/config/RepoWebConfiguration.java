package org.sagebionetworks.repo.web.config;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.audit.AccessRecorder;
import org.sagebionetworks.repo.manager.config.SimpleTriggerBuilder;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor;
import org.sagebionetworks.repo.manager.monitoring.DataSourcePoolMonitor.ApplicationType;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@Configuration
public class RepoWebConfiguration implements WebMvcConfigurer {
	
	private static final long DB_MONITOR_INTERVAL = 10_000;
	
	private ObjectTypeSerializer exceptionSerializer;
	private Consumer consumer;
	private StackConfiguration config;
	
	public RepoWebConfiguration(ObjectTypeSerializer exceptionSerializer, Consumer consumer, StackConfiguration config) {
		this.exceptionSerializer = exceptionSerializer;
		this.consumer = consumer;
		this.config = config;
	}
		
	// Override the default ExceptionHandlerExceptionResolver used by spring so that we can customize the message converters and the content negotiation
	@Bean
	public ExceptionHandlerExceptionResolver controllerExceptionHandlerResolver() {
		ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
		
		resolver.setMessageConverters(Collections.singletonList(exceptionSerializer));
		resolver.setContentNegotiationManager(exceptionContentNegotiationManager());
		
		return resolver;
		
	}
	
	@Bean
	public SimpleTriggerFactoryBean dataSourceMonitorTrigger(Map<String, BasicDataSource> dataSources) {
		return new SimpleTriggerBuilder()
			.withTargetObject(new DataSourcePoolMonitor(ApplicationType.repository, dataSources, consumer, config))
			.withTargetMethod("collectMetrics")
			.withRepeatInterval(DB_MONITOR_INTERVAL)
			.withStartDelay(DB_MONITOR_INTERVAL)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean accessRecorderTrigger(AccessRecorder accessRecorder) {
		return new SimpleTriggerBuilder()
				.withTargetObject(accessRecorder)
				.withTargetMethod("timerFired")
				.withRepeatInterval(957)
				.withStartDelay(13)
				.build();
	}
	
	// The following beans are not exposed as they are used in place here
	
	private ContentNegotiationManager exceptionContentNegotiationManager() {
		return new ContentNegotiationManager(exceptionContentNegotiationStrategy());
	}
		
	private ContentNegotiationStrategy exceptionContentNegotiationStrategy() {
		return new ExceptionContentNegotiationStrategy(exceptionSerializer.getSupportedMediaTypes());
	}
	
	
}
