package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

/**
 * This filter is our last chance to log any type of unexpected error. Errors
 * that occur at the controller level or below are already well handled. All
 * errors from the controllers or lower are already captured and converted to
 * status codes with error messages, so those exceptions will never be seen
 * here. This filter is designed to capture unexpected errors that occur above
 * the controller in all other filters or interceptors. See PLFM-3204 &
 * PLFM-3205
 * 
 * @author John
 * 
 */
@Component("unexpectedExceptionFilter")
public class UnexpectedExceptionFilter implements Filter {
	
	private static final String CLOUD_WATCH_NAMESPACE_PREFIX = "UnexpectedExceptionFilter";
	private static final String CLOUD_WATCH_METRIC_NAME = "UnhandledException";
	private static final String CLOUD_WATCH_DIMENSION_EXCEPTION = "exceptionClass";
	private static final String CLOUD_WATCH_DIMENSION_MESSAGE = "message";

	private static Log log = LogFactory.getLog(UnexpectedExceptionFilter.class);
	
	private StackConfiguration config;
	private Consumer consumer;
	
	@Autowired
	public UnexpectedExceptionFilter(StackConfiguration config, Consumer consumer) {
		this.config = config;
		this.consumer = consumer;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		try {
			chain.doFilter(request, response);
		} catch (Throwable e) {
			Date timestamp = new Date();

			HttpServletRequestData data = new HttpServletRequestData((HttpServletRequest) request);
			
			// capture the full stack trace and request data.
			log.error(data.toString(), e);
			
			List<ProfileData> profileData = new ArrayList<>();
			
			profileData.add(generateProfileData(timestamp, config.getStackInstance(), null));
			profileData.add(generateProfileData(timestamp, config.getStackInstance(), e));
			
			consumer.addProfileData(profileData);
			
			/*
			 * Assume the server will not recover from an error so report generic 500.
			 */
			HttpServletResponse res = (HttpServletResponse) response;
			res.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			res.setContentType(MediaType.APPLICATION_JSON_VALUE);
			res.getWriter().println(AuthorizationConstants.REASON_SERVER_ERROR);
		}
	}
	
	private static ProfileData generateProfileData(Date timestamp, String stackInstance, Throwable e) {
		ProfileData logEvent = new ProfileData();

		logEvent.setNamespace(String.format("%s - %s", CLOUD_WATCH_NAMESPACE_PREFIX, stackInstance));
		logEvent.setName(CLOUD_WATCH_METRIC_NAME);
		logEvent.setValue(1.0);
		logEvent.setUnit(StandardUnit.Count.toString());
		logEvent.setTimestamp(timestamp);
		
		if (e != null) {
			Map<String, String> dimensions = ImmutableMap.of(
					CLOUD_WATCH_DIMENSION_EXCEPTION, e.getClass().getSimpleName(),
					CLOUD_WATCH_DIMENSION_MESSAGE, MetricUtils.stackTracetoString(e)
			);
			logEvent.setDimension(dimensions);
		}
		
		return logEvent;
	}

	@Override
	public void destroy() {
	}

}
