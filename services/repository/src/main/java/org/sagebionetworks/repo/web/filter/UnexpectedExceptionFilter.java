package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.controller.BaseControllerExceptionHandlerAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * This filter is our last chance to log any type of unexpected error. Errors
 * that occur at the controller level or below are already well handled. All
 * errors from the controllers or lower are already captured and converted to
 * status codes with error messages, so those exceptions will never be seen
 * here. This filter is designed to capture unexpected errors that occur above
 * the controller in all other filters or interceptors. See PLFM-3204 &
 * PLFM-3205. This implementation reuses the same {@link HandlerExceptionResolver} 
 * configured in spring, so that the exception handling logic is implemented in a single place 
 * (E.g. The {@link BaseControllerExceptionHandlerAdvice}). When an exception occurs at this
 * an event is logged in cloudwatch.
 * 
 * @author John
 * 
 */
@Component("unexpectedExceptionFilter")
public class UnexpectedExceptionFilter extends OncePerRequestFilter {
	
	private static final String CLOUD_WATCH_NAMESPACE_PREFIX = "UnexpectedExceptionFilter";
	private static final String CLOUD_WATCH_METRIC_NAME = "UnhandledException";
	private static final String CLOUD_WATCH_DIMENSION_EXCEPTION = "exceptionClass";
	private static final String CLOUD_WATCH_DIMENSION_URI = "requestUri";
	private static final String CLOUD_WATCH_DIMENSION_MESSAGE = "message";

	private static Log log = LogFactory.getLog(UnexpectedExceptionFilter.class);
	
	private StackConfiguration config;
	private Consumer consumer;
	private List<HandlerExceptionResolver> exceptionResolvers;
	
	@Autowired
	public UnexpectedExceptionFilter(StackConfiguration config, Consumer consumer) {
		this.config = config;
		this.consumer = consumer;
	}
	
	@Autowired
	public void configureExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		if (exceptionResolvers == null || exceptionResolvers.isEmpty()) {
			throw new IllegalStateException("No exception resolver found");
		}
		this.exceptionResolvers = exceptionResolvers;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException{
		try {
			chain.doFilter(request, response);
		} catch (Exception e) {
			logUnhandledException(request, e);
			
			boolean resolved = tryResolveException(request, response, e);			
			
			if (!resolved) {
				// No resolver was configured for the exception, just throw back a generic 500
				response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.getWriter().println(AuthorizationConstants.REASON_SERVER_ERROR);
			}
		}
	}
	
	private void logUnhandledException(HttpServletRequest request, Throwable e) {
		Date timestamp = new Date();

		HttpServletRequestData data = new HttpServletRequestData((HttpServletRequest) request);
		
		// capture the full stack trace and request data.
		log.error(data.toString(), e);
		
		List<ProfileData> profileData = new ArrayList<>();
		
		profileData.add(generateProfileData(data, timestamp, e, false));
		profileData.add(generateProfileData(data, timestamp, e, true));
		
		consumer.addProfileData(profileData);
	}
	
	/**
	 * Delegates the handling of the exception to the handlers we setup for the dispatcher servlet
	 */
	private boolean tryResolveException(HttpServletRequest request, HttpServletResponse response, Exception e) {
		boolean resolved = false;
		for (HandlerExceptionResolver exceptionResolver : exceptionResolvers) {
			
			// Try to resolve this exception, and simply stops whenever a revolver was able to handle the exception
			resolved = exceptionResolver.resolveException(request, response, null, e) != null;
			
			if (resolved) {
				break;
			}
		}
		return resolved;
	}
	
	private ProfileData generateProfileData(HttpServletRequestData requestData, Date timestamp, Throwable e, boolean withMessage) {
		ProfileData logEvent = new ProfileData();

		String stackInstance = config.getStackInstance();
		
		logEvent.setNamespace(String.format("%s - %s", CLOUD_WATCH_NAMESPACE_PREFIX, stackInstance));
		logEvent.setName(CLOUD_WATCH_METRIC_NAME);
		logEvent.setValue(1.0);
		logEvent.setUnit(StandardUnit.Count.toString());
		logEvent.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();
		
		dimensions.put(CLOUD_WATCH_DIMENSION_URI, requestData.getUri());
		dimensions.put(CLOUD_WATCH_DIMENSION_EXCEPTION, e.getClass().getName());
		
		if (withMessage) {
			dimensions.put(CLOUD_WATCH_DIMENSION_MESSAGE, MetricUtils.stackTracetoString(e));
		}

		logEvent.setDimension(dimensions);
		
		return logEvent;
	}

}
