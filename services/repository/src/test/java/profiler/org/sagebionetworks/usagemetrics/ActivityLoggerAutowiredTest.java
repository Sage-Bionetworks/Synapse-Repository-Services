package profiler.org.sagebionetworks.usagemetrics;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.web.controller.ActivityLoggerTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:activityLoggerAutowired-spb.xml" })
public class ActivityLoggerAutowiredTest {

	@Autowired
	ActivityLoggerTestHelper testClass;

	@Autowired
	ActivityLogger activityLoggerSpy;

	HttpServletRequest mockRequest;
	private static Vector<String> headerNames = new Vector<String>();

	static {
		headerNames.addElement("user-agent");
		headerNames.addElement("sessiontoken");
		headerNames.addElement("header");
	}

	@Before
	public void before() throws Exception {
		mockRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(mockRequest.getHeaderNames()).thenReturn(headerNames.elements());
		Mockito.when(mockRequest.getHeader(Mockito.anyString())).thenReturn("");
	}

	/**
	 * Test that validates the spring configuration is correct and that the
	 * activityLogger is actually called when the ActivityLoggerTestHelper method is.
	 * This also tests that the pointcut expression is correct.
	 * @throws Throwable
	 */
	@Test
	public void testConfig() throws Throwable {
		testClass.testAnnotationsMethod("uniqueEntityId", "uniqueUserId", mockRequest);

		verify(activityLoggerSpy).doBasicLogging((ProceedingJoinPoint) anyObject());
	}
}
