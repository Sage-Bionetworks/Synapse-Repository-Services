package org.sagebionetworks.logging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aspectj.lang.reflect.MethodSignature;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.BeforeClass;
import org.junit.Test;


public class SynapseLoggingUtilsTest {
	private static Method SIMPLE_METHOD = null;
	private static Method ANNOTATION_METHOD = null;

	private static final String SIMPLE_METHOD_NAME = "testMethod";
	private static final String ANNOTATION_METHOD_NAME = "testAnnotationsMethod";

	private static final String[]  SIMPLE_METHOD_PARAM_NAMES = {"arg1", "arg2"};
	private static final String[]  ANNOTATION_METHOD_PARAM_NAMES = {"id", "userId"};

	private static final Class<?>[] SIMPLE_METHOD_ARG_TYPES = new Class<?>[] {String.class, Integer.class};
	private static final Class<?>[] ANNOTATION_METHOD_ARG_TYPES = new Class<?>[] {String.class, String.class};

	private static final Object[] SIMPLE_METHOD_ARGS = new Object[] {"testarg", new Integer(12)};
	private static final Object[] ANNOTATION_METHOD_ARGS = new Object[] {"entityIdval", "userIdval"};

	private static final String ARG_FMT_STRING = "%s=%s&%s=%s";

	private static final String SIMPLE_ARG_STRING = String.format(
			ARG_FMT_STRING, SIMPLE_METHOD_PARAM_NAMES[0],
			SIMPLE_METHOD_ARGS[0], SIMPLE_METHOD_PARAM_NAMES[1],
			SIMPLE_METHOD_ARGS[1]);

	private static final String ANNOTATION_ARG_STRING = String.format(
			ARG_FMT_STRING, ANNOTATION_METHOD_PARAM_NAMES[0],
			ANNOTATION_METHOD_ARGS[0], ANNOTATION_METHOD_PARAM_NAMES[1],
			ANNOTATION_METHOD_ARGS[1]);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Class<LoggingUtilsTestHelper> clazz = LoggingUtilsTestHelper.class;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equalsIgnoreCase(SIMPLE_METHOD_NAME)) {
				SIMPLE_METHOD = method;
			}
			if (method.getName().equalsIgnoreCase(ANNOTATION_METHOD_NAME)) {
				ANNOTATION_METHOD = method;
			}
		}
	}

	private static final String VALID_DATE = "2012-08-06 18:36:22,961";
	private static final String VALID_LEVEL = " [TRACE] - ";
	private static final String VALID_CONTROLLER = "AFakeController/andFakeMethod";
	private static final String VALID_PROPERTIES = "?fakeProp=true&testProp=%.-*_+abcABC123";

	@Test(expected=IllegalArgumentException.class)
	public void testParseEventFailDate() throws UnsupportedEncodingException {
		String line = "Not a Synapse Event";
		SynapseLoggingUtils.parseSynapseEvent(line);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testParseEventFailLevel() throws UnsupportedEncodingException {
		String line = VALID_DATE+VALID_LEVEL; 
		SynapseLoggingUtils.parseSynapseEvent(line);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testParseEventFailController() throws UnsupportedEncodingException {
		String line = VALID_DATE+VALID_LEVEL+VALID_CONTROLLER; 
		SynapseLoggingUtils.parseSynapseEvent(line);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testParseEventFailProperties() throws UnsupportedEncodingException {
		String line = VALID_DATE+VALID_LEVEL+VALID_CONTROLLER+VALID_PROPERTIES; 
		SynapseLoggingUtils.parseSynapseEvent(line);
	}
	
	@Test
	public void testParseSynapseEvent() throws UnsupportedEncodingException {
		String controller = "AccessRequirementController";
		String method = "getUnfulfilledAccessRequirement";

		StringBuilder builder = new StringBuilder(String.format(
				"%s%s%s/%s?", VALID_DATE, VALID_LEVEL, controller, method));

		String latencyLabel = "latency";
		int latencyValue = 6;
		builder.append(String.format("%s=%d", latencyLabel, latencyValue));

		Map<String, String> propMap = new HashMap<String, String>();
		propMap.put("userId", "Geoff.Shannon@sagebase.org");
		propMap.put("entityId", "syn114138");
		propMap.put("request",
				"org.sagebionetworks.authutil.ModParamHttpServletRequest@5cba727");
		propMap.put("header", "header={sessiontoken=[0BMBOTJ7Wvvhz0Y4d1RMnw00], content-type=[application/json], accept=[application/json], content-length=[92], host=[localhost:8080], connection=[Keep-Alive]}");
		for (Entry<String, String> entry : propMap.entrySet()) {
			builder.append("&");
			builder.append(entry.getKey());
			builder.append("=");
			builder.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		String line = builder.toString();
		SynapseEvent event = SynapseLoggingUtils.parseSynapseEvent(line);

		DateTimeFormatter dateFormatter = SynapseLoggingUtils.DATE_FORMATTER;
		DateTime time = dateFormatter.parseDateTime(VALID_DATE);

		assertEquals(time, event.getTimeStamp());
		assertEquals(latencyValue, event.getLatency());
		assertEquals(controller, event.getController());
		assertEquals(method, event.getMethodName());

		for (String key : propMap.keySet()) {
			assertEquals(propMap.get(key), URLDecoder.decode(event.getProperties().get(key), "UTF-8"));
		}
	}

	@Test
	public void testGetArgsNoMethod() throws Exception {
		String simpleLog = doGetArgs(null, SIMPLE_METHOD_NAME, SIMPLE_METHOD_PARAM_NAMES, SIMPLE_METHOD_ARG_TYPES, SIMPLE_METHOD_ARGS);
		assertEquals(Arrays.toString(SIMPLE_METHOD_ARGS), simpleLog);
	}

	@Test
	public void testGetArgsNoArgs() throws Exception {
		String simpleLog = doGetArgs(SIMPLE_METHOD, SIMPLE_METHOD_NAME, SIMPLE_METHOD_PARAM_NAMES, SIMPLE_METHOD_ARG_TYPES,
				new Object[]{null, null});
		assertEquals(String.format(ARG_FMT_STRING,
				SIMPLE_METHOD_PARAM_NAMES[0], "null",
				SIMPLE_METHOD_PARAM_NAMES[1], "null"), simpleLog);
	}

	@Test
	public void testGetArgsEncoding() throws Exception {
		String simpleLog = doGetArgs(SIMPLE_METHOD, SIMPLE_METHOD_NAME, SIMPLE_METHOD_PARAM_NAMES, SIMPLE_METHOD_ARG_TYPES,
				new Object[] {"{athing=another, two=2}", "?&= "});
		String[] encoded = {"%7Bathing%3Danother%2C+two%3D2%7D", "%3F%26%3D+"};
		assertEquals(String.format(ARG_FMT_STRING,
				SIMPLE_METHOD_PARAM_NAMES[0], encoded[0],
				SIMPLE_METHOD_PARAM_NAMES[1], encoded[1]), simpleLog);
	}

	@Test
	public void testGetArgs() throws Exception {
		String simpleLog = doGetArgs(SIMPLE_METHOD, SIMPLE_METHOD_NAME, SIMPLE_METHOD_PARAM_NAMES, SIMPLE_METHOD_ARG_TYPES, SIMPLE_METHOD_ARGS);
		assertEquals(SIMPLE_ARG_STRING, simpleLog);
		String annotationsLog = doGetArgs(ANNOTATION_METHOD, ANNOTATION_METHOD_NAME, ANNOTATION_METHOD_PARAM_NAMES,
				ANNOTATION_METHOD_ARG_TYPES, ANNOTATION_METHOD_ARGS);
		assertEquals(ANNOTATION_ARG_STRING, annotationsLog);
	}

	private String doGetArgs(Method method, String methodName, String[] methodArgNames, Class<?>[] methodArgTypes, Object[] methodArgs)
			throws NoSuchMethodException, UnsupportedEncodingException {

		MethodSignature mockSig = mock(MethodSignature.class);
		when(mockSig.getName()).thenReturn(methodName);
		when(mockSig.getParameterNames()).thenReturn(methodArgNames);
		when(mockSig.getMethod()).thenReturn(method);

		return SynapseLoggingUtils.makeArgString(mockSig, methodArgs);
	}

}
