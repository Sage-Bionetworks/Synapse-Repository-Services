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

	private static final String TOPIC_ARN_FIELD = "TopicArn";
	private static final String MESSAGE_FIELD = "Message";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			// Does not fail for properties that are not mapped
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());

	public static SESJsonNotification parseSQSMessage(String messageBody) throws IOException {
		String notificationBody = extractNotificationBody(messageBody);
		return OBJECT_MAPPER.readValue(notificationBody, SESJsonNotification.class);
	}

	private static String extractNotificationBody(String messageBody) throws IOException {
		JsonNode root = OBJECT_MAPPER.readTree(messageBody);

		// If a message comes from a topic, the body of the message is in the "Message" property as a plain string
		if (root.has(TOPIC_ARN_FIELD) && root.has(MESSAGE_FIELD)) {
			return root.get(MESSAGE_FIELD).textValue();
		}
		
		return messageBody;
	}

	// For testing SES notifications

	public static String loadMessageFromClasspath(String suffix) throws IOException {
		return loadJsonFromClasspath("ses/message_" + suffix);
	}

	public static String loadNotificationFromClasspath(String suffix) throws IOException {
		return loadJsonFromClasspath("ses/notification_" + suffix);
	}

	private static String loadJsonFromClasspath(String fileName) throws IOException {
		String path = fileName + ".json";
		try (InputStream is = SESNotificationUtils.class.getClassLoader().getResourceAsStream(path)) {
			return IOUtils.toString(is, StandardCharsets.UTF_8);
		}
	}

}
