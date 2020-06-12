package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class FilterHelper {
	private static final String CLOUD_WATCH_NAMESPACE_PREFIX = "Authentication";
	private static final String CLOUD_WATCH_METRIC_NAME = "BadCredentials";
	private static final String CLOUD_WATCH_DIMENSION_FILTER = "filterClass";
	private static final String CLOUD_WATCH_DIMENSION_MESSAGE = "message";
	private static final String CLOUD_WATCH_UNIT_COUNT = StandardUnit.Count.toString();
	
	private StackConfiguration config;
	private Consumer consumer;
	private Log logger = LogFactory.getLog(getClass());
		
	public FilterHelper(StackConfiguration config, Consumer consumer) {
		this.config = config;
		this.consumer = consumer;
	}
	
	/**
	 * Rejects the http request due to the given exception sending a 401 in the response with the exception message.
	 * If {@link #reportBadCredentialsMetric()} is true sends a bad credentials metric to cloud watch
	 * 
	 * @param response
	 * @param ex
	 * @throws IOException
	 */
	public void rejectRequest(boolean reportBadCredentialsMetric, HttpServletResponse response, Exception ex) throws IOException {
		if (reportBadCredentialsMetric) {
			logger.error(ex.getMessage(), ex);

			// We log in cloudwatch the stack trace of the exception
			String stackTraceString = MetricUtils.stackTracetoString(ex);
			sendBadCredentialMetric(consumer, getClass().getName(), config.getStackInstance(), stackTraceString);
		}

		HttpAuthUtil.reject(response, ex.getMessage());
	}

	/**
	 * Rejects a the http request and sends a 401 in the response with the given
	 * message as the reason, if {@link #reportBadCredentialsMetric()} is true sends
	 * a bad credentials metric to cloud watch
	 * 
	 * @param response
	 * @param message The message to be returned in the response
	 * @throws IOException
	 */
	public void rejectRequest(boolean reportBadCredentialsMetric, HttpServletResponse response, String message) throws IOException {
		if (reportBadCredentialsMetric) {
			sendBadCredentialMetric(consumer, getClass().getName(), config.getStackInstance(), message);
		}

		HttpAuthUtil.reject(response, message);
	}


	private static void sendBadCredentialMetric(Consumer consumer, String filterClass, String stackInstance, String message) {
		
		Date timestamp = new Date();
		
		List<ProfileData> data = new ArrayList<>();
		
		// Note: Setting dimensions defines a new metric since the metric itself is identified by the name and dimensions
		// (See https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension)
		// 
		// We send two different metrics (with the same timestamp) one that includes the message so that we can quickly inspect it
		// and one without the message so that an alarm can be created (since we don't know the message in advance it would be impossible
		// to create an alarm).
		
		data.add(generateProfileData(timestamp, filterClass, stackInstance, null));
		
		if (!StringUtils.isBlank(message)) {
			data.add(generateProfileData(timestamp, filterClass, stackInstance, message));
		}
		
		consumer.addProfileData(data);
	}
	
	private static ProfileData generateProfileData(Date timestamp, String filterClass, String stackInstance, String message) {
		ProfileData logEvent = new ProfileData();

		logEvent.setNamespace(String.format("%s - %s", CLOUD_WATCH_NAMESPACE_PREFIX, stackInstance));
		logEvent.setName(CLOUD_WATCH_METRIC_NAME);
		logEvent.setValue(1.0);
		logEvent.setUnit(CLOUD_WATCH_UNIT_COUNT);
		logEvent.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();
		
		dimensions.put(CLOUD_WATCH_DIMENSION_FILTER, filterClass);
		
		if (message != null) {
			dimensions.put(CLOUD_WATCH_DIMENSION_MESSAGE, message);
		};
		
		logEvent.setDimension(dimensions);
		
		return logEvent;
	}
}
