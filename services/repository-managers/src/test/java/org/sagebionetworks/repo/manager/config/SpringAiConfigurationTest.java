package org.sagebionetworks.repo.manager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.util.json.JsonParser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Verifies that {@link SpringAiConfiguration#configureToolArgumentJsonParsing()} makes Spring AI's
 * two shared tool-argument parsers tolerant of unescaped control characters. Bedrock/Claude emit
 * multi-line text (e.g. the runPython {@code script}, or a multi-line SQL query delegated to a
 * specialist) with raw newlines, which the strict parsers reject with "Illegal unquoted character
 * (CTRL-CHAR, code 10)". The two parsers are exercised on different turns:
 * <ul>
 * <li>{@link JsonParser#getObjectMapper()} — used by {@code MethodToolCallback} when invoking a tool.</li>
 * <li>{@link ModelOptionsUtils#OBJECT_MAPPER} — used by {@code BedrockProxyChatModel.createRequest}
 * (via {@code ModelOptionsUtils.jsonToMap}) when re-serializing a prior tool-call's arguments into the
 * next request.</li>
 * </ul>
 */
public class SpringAiConfigurationTest {

	// A tool-call arguments payload with a raw (unescaped) newline inside the string value,
	// exactly as the model emits multi-line text (Python script, SQL query, etc.).
	private static final String MULTILINE_TOOL_ARGS = "{\"script\": \"import os\nprint(os.getcwd())\"}";

	@Test
	public void testStrictParserRejectsUnescapedControlChars() {
		// A strict, default Jackson mapper (matching Spring AI's out-of-the-box behavior) must reject
		// the raw newline. This proves the payload actually exercises the failure mode.
		ObjectMapper strictMapper = new ObjectMapper();
		assertThrows(JsonProcessingException.class,
				() -> strictMapper.readValue(MULTILINE_TOOL_ARGS, new TypeReference<Map<String, Object>>() {}));
	}

	@Test
	public void testConfigureToolArgumentJsonParsingAllowsMultilineScript() throws Exception {
		// call under test
		new SpringAiConfiguration().configureToolArgumentJsonParsing();

		// After configuration, the tool-invocation parser (the one MethodToolCallback uses) parses the
		// multi-line script without throwing, and preserves the newline in the value.
		Map<String, Object> parsed = JsonParser.getObjectMapper()
				.readValue(MULTILINE_TOOL_ARGS, new TypeReference<Map<String, Object>>() {});

		assertEquals("import os\nprint(os.getcwd())", parsed.get("script"));
	}

	@Test
	public void testConfigureToolArgumentJsonParsingAllowsMultilineInModelOptionsUtils() {
		// call under test
		new SpringAiConfiguration().configureToolArgumentJsonParsing();

		// After configuration, the request-building parser (ModelOptionsUtils.jsonToMap, used by
		// BedrockProxyChatModel.createRequest when re-serializing a prior tool-call's arguments) parses
		// the multi-line payload without throwing, and preserves the newline in the value.
		Map<String, Object> parsed = ModelOptionsUtils.jsonToMap(MULTILINE_TOOL_ARGS);

		assertEquals("import os\nprint(os.getcwd())", parsed.get("script"));
	}
}
