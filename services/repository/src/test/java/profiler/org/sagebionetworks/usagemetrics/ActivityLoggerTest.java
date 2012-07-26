package profiler.org.sagebionetworks.usagemetrics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
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

	private static final String SIMPLE_ARG_STRING = SIMPLE_METHOD_PARAM_NAMES[0]+"="
			+ SIMPLE_METHOD_ARGS[0] + ","+SIMPLE_METHOD_PARAM_NAMES[1]+"=" + SIMPLE_METHOD_ARGS[1];

	private static final String ANNOTATION_ARG_STRING = ANNOTATION_METHOD_PARAM_NAMES[0]+"="
			+ ANNOTATION_METHOD_ARGS[0] + ","+ANNOTATION_METHOD_PARAM_NAMES[1]+"="
			+ ANNOTATION_METHOD_ARGS[1];

	private static final String ANNOTATION_ARG_STRING_WITH_ANNOTATIONS = ANNOTATION_METHOD_PARAM_NAMES[0]+"{@org.springframework.web.bind.annotation.PathVariable(value=)}="
			+ ANNOTATION_METHOD_ARGS[0] + ","+ANNOTATION_METHOD_PARAM_NAMES[1]+"{@org.springframework.web.bind.annotation.RequestParam(value=userId, required=false, defaultValue=)}="
			+ ANNOTATION_METHOD_ARGS[1];

	@Autowired
	ActivityLogger activityLogger;

	private static Log mockLog = mock(Log.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ActivityLogger.setLog(mockLog);

		Class<TestClass> clazz = TestClass.class;
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
		activityLogger.setShouldLogAnnotations(false);
	}

	/**
	 * \@RequestParam spits out a bunch of garbage if the default is left unchanged.
	 * This is intentional behavior, on their part, and so we have to work around it somehow.
	 * @throws Exception
	 */
	@Test
	public void testGetArgs() throws Exception {
		String simpleLog = doGetArgs(SIMPLE_METHOD, SIMPLE_METHOD_NAME, SIMPLE_METHOD_PARAM_NAMES, SIMPLE_METHOD_ARG_TYPES, SIMPLE_METHOD_ARGS);
		assertEquals(SIMPLE_ARG_STRING, simpleLog);
		String annotationsLog = doGetArgs(ANNOTATION_METHOD, ANNOTATION_METHOD_NAME, ANNOTATION_METHOD_PARAM_NAMES,
				ANNOTATION_METHOD_ARG_TYPES, ANNOTATION_METHOD_ARGS);
		assertEquals(ANNOTATION_ARG_STRING, annotationsLog);
	}

	@Test
	public void testGetArgsWithAnnotations() throws Exception {
		activityLogger.setShouldLogAnnotations(true);

		String annotationsLog = doGetArgs(ANNOTATION_METHOD, ANNOTATION_METHOD_NAME, ANNOTATION_METHOD_PARAM_NAMES,
				ANNOTATION_METHOD_ARG_TYPES, ANNOTATION_METHOD_ARGS);

		assertEquals(ANNOTATION_ARG_STRING_WITH_ANNOTATIONS, annotationsLog);
	}

	private String doGetArgs(Method method, String methodName, String[] methodArgNames, Class<?>[] methodArgTypes, Object[] methodArgs)
			throws NoSuchMethodException {
		Class<? extends TestClass> classTestClass = TestClass.class;

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

		when(mockSig.getDeclaringType()).thenReturn(TestClass.class);
		when(mockSig.getMethod()).thenReturn(ANNOTATION_METHOD);
		when(mockSig.getName()).thenReturn(ANNOTATION_METHOD_NAME);
		when(mockSig.getParameterNames()).thenReturn(ANNOTATION_METHOD_PARAM_NAMES);

		activityLogger.doBasicLogging(mockPjp);

		ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
		verify(mockLog).trace(arg.capture());
		System.out.println(arg.getValue());
		String expected = "TestClass.testAnnotationsMethod(id=entityIdval,userId=userIdval)";
		assertEquals(expected, arg.getValue().split(" ")[1]);
	}
}
