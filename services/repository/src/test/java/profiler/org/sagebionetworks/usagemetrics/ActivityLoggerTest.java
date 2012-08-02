package profiler.org.sagebionetworks.usagemetrics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.repo.web.controller.ActivityLoggerTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activityLogger-spb.xml" })
public class ActivityLoggerTest {

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

	@Autowired
	ActivityLogger activityLogger;

	private static Log mockLog;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Class<ActivityLoggerTestHelper> clazz = ActivityLoggerTestHelper.class;
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

	@Before
	public void before() throws Exception {
		mockLog = mock(Log.class);
		ActivityLogger.setLog(mockLog);
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
		System.out.println(simpleLog);
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
		Class<? extends ActivityLoggerTestHelper> classTestClass = ActivityLoggerTestHelper.class;

		MethodSignature mockSig = mock(MethodSignature.class);
		when(mockSig.getName()).thenReturn(methodName);
		when(mockSig.getParameterNames()).thenReturn(methodArgNames);
		when(mockSig.getMethod()).thenReturn(method);

		return activityLogger.getArgs(classTestClass, mockSig, methodArgs);
	}

	@Test
	public void testDoBasicLogging() throws Throwable {
		ProceedingJoinPoint mockPjp = mock(ProceedingJoinPoint.class);
		MethodSignature mockSig = mock(MethodSignature.class, RETURNS_DEEP_STUBS);

		when(mockPjp.getArgs()).thenReturn(ANNOTATION_METHOD_ARGS);
		when(mockPjp.getSignature()).thenReturn(mockSig);

		when(mockSig.getDeclaringType()).thenReturn(ActivityLoggerTestHelper.class);
		when(mockSig.getMethod()).thenReturn(ANNOTATION_METHOD);
		when(mockSig.getName()).thenReturn(ANNOTATION_METHOD_NAME);
		when(mockSig.getParameterNames()).thenReturn(ANNOTATION_METHOD_PARAM_NAMES);

		activityLogger.doBasicLogging(mockPjp);

		ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
		verify(mockLog).trace(arg.capture());

		String[] methodAndArgs = arg.getValue().split("\\?");
		String[] classAndMethod = methodAndArgs[0].split("/");

		String latencyArg = "latency=0&";
		int indexOf = methodAndArgs[1].indexOf(latencyArg);

		assertEquals(ANNOTATION_ARG_STRING, methodAndArgs[1].substring(indexOf + latencyArg.length()));

		assertEquals(ActivityLoggerTestHelper.class.getSimpleName(), classAndMethod[0]);
		assertEquals(classAndMethod[1], ANNOTATION_METHOD_NAME);
	}

	@Test
	public void testDoBasicLoggingJoinPointNotMethod() throws Throwable {
		ProceedingJoinPoint mockPjp = mock(ProceedingJoinPoint.class);
		Signature mockSig = mock(Signature.class, CALLS_REAL_METHODS);

		when(mockPjp.getSignature()).thenReturn(mockSig);

		activityLogger.doBasicLogging(mockPjp);

		verify(mockLog).error(anyString());
	}
}
