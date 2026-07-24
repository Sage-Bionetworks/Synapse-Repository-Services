package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Renders the real notification templates with a VelocityEngine configured to match the
 * Spring-configured bean (strict reference mode), to catch templates that reference variables not
 * supplied in the context map. The mock-based unit tests only verify the context that is passed to
 * the sender, never the rendering.
 *
 * @see org.sagebionetworks.repo.manager.config.ManagerConfiguration#velocityEngine()
 */
public class TemplatedMessageSenderImplIntegrationTest {

    private TemplatedMessageSenderImpl templatedMessageSender;

	@BeforeEach
	public void before() {
		// Mirrors ManagerConfiguration.velocityEngine(): load templates from the classpath and enable
		// strict reference mode so that referencing an undefined variable throws rather than rendering empty.
		VelocityEngine velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, true);
		velocityEngine.init();
		// Only the VelocityEngine is exercised by buildMessageBody, the other collaborators are not needed
		templatedMessageSender = new TemplatedMessageSenderImpl(velocityEngine, null, null);
	}

	@Test
	public void testBuildMessageBodyForInactivityWarning() {
		Map<String, Object> context = Map.of(
				"displayName", "Alice",
				"expiryDate", "2026-06-19"
		);

		// call under test
		String result = templatedMessageSender.buildMessageBody("message/InactivityWarningNotification.html.vtl",
				StandardCharsets.UTF_8, context);

		// Rendering under strict reference mode would have thrown if a referenced variable was missing
		assertTrue(result.contains("Alice"), "Expected displayName to be substituted");
		assertTrue(result.contains("2026-06-19"), "Expected expiryDate to be substituted");
	}

}
