package org.sagebionetworks.repo.model.ses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class SESNotificationUtils {

	private static final String MESSAGE_FIELD = "Message";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			// Does not fail for properties that are not mapped
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());

	public static String extractNotificationBody(String messageBody) throws IOException {
		JsonNode root = OBJECT_MAPPER.readTree(messageBody);

		if (!root.has(MESSAGE_FIELD)) {
			throw new IllegalArgumentException("Could not find \"Message\" field in the message body: " + messageBody);
		}

		JsonNode messageNode = root.get(MESSAGE_FIELD);

		return messageNode.textValue();
	}

	public static SESJsonNotification parseNotification(String notificationBody) throws IOException {
		return OBJECT_MAPPER.readValue(notificationBody, SESJsonNotification.class);
	}
	
	// For testing SES notifications
	
	public static String loadMessageFromClasspath(String useCase) throws IOException {
		return loadJsonFromClasspath("message_" + useCase);
	}

	private static String loadJsonFromClasspath(String fileName) throws IOException {
		String path = "ses/" + fileName + ".json";
		try (InputStream is = SESNotificationUtils.class.getClassLoader().getResourceAsStream(path)) {
			return IOUtils.toString(is, StandardCharsets.UTF_8);
		}
	}

}
