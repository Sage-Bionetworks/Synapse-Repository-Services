package org.sagebionetworks.repo.model.ses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SESNotificationUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			// Does not fail for properties that are not mapped
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.registerModule(new JavaTimeModule());

	public static SESJsonNotification parseNotification(String notificationBody) throws IOException {
		return OBJECT_MAPPER.readValue(notificationBody, SESJsonNotification.class);
	}
	
	public static String loadNotificationFromClasspath(String messageId) throws IOException {
		String fileName = "ses/" + messageId + ".json";
		try (InputStream is = SESNotificationUtils.class.getClassLoader().getResourceAsStream(fileName)) {
			return IOUtils.toString(is, StandardCharsets.UTF_8);
		}
	}

}
