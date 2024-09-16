package org.sagebionetworks.repo.manager.agent.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParameterUtilsTest {

	List<Parameter> params;

	@BeforeEach
	public void before() {
		params = List.of(new Parameter("name1", "string", "string1"), new Parameter("name1", "integer", "1234"),
				new Parameter("name2", "string", "string2"));
	}

	@Test
	public void testExtractParameter() {
		// call under test
		assertEquals(Optional.of(1234), ParameterUtils.extractParameter(Integer.class, "name1", params));
		assertEquals(Optional.of("string1"), ParameterUtils.extractParameter(String.class, "name1", params));
		assertEquals(Optional.of("string2"), ParameterUtils.extractParameter(String.class, "name2", params));
		assertEquals(Optional.empty(), ParameterUtils.extractParameter(Integer.class, "name2", params));
		assertEquals(Optional.empty(), ParameterUtils.extractParameter(String.class, "nope", params));
	}

	@Test
	public void testExtractParameterWithUnknownType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			ParameterUtils.extractParameter(Object.class, "name", params);
		}).getMessage();
		assertEquals("Unsupported Parameter type: java.lang.Object", message);
	}

	@Test
	public void testExtractParameterWithNullType() {
		String message = assertThrows(NullPointerException.class, () -> {
			// call under test
			ParameterUtils.extractParameter(null, "name", params);
		}).getMessage();
		assertEquals("type", message);
	}

	@Test
	public void testExtractParameterWithNullName() {
		String message = assertThrows(NullPointerException.class, () -> {
			// call under test
			ParameterUtils.extractParameter(String.class, null, params);
		}).getMessage();
		assertEquals("name", message);
	}

	@Test
	public void testExtractParameterWithNullParams() {
		String message = assertThrows(NullPointerException.class, () -> {
			// call under test
			ParameterUtils.extractParameter(String.class, "name", null);
		}).getMessage();
		assertEquals("parameters", message);
	}
}
