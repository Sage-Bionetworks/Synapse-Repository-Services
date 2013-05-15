package org.sagebionetworks.profiler.usagemetrics;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.profiler.usagemetrics.ActivityLogger;
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

	/**
	 * Test that validates the spring configuration is correct and that the
	 * activityLogger is actually called when the ActivityLoggerTestHelper method is.
	 * This also tests that the pointcut expression is correct.
	 * @throws Throwable
	 */
	@Test
	public void testConfig() throws Throwable {
		testClass.testAnnotationsMethod("uniqueEntityId", "uniqueUserId");

		verify(activityLoggerSpy).doBasicLogging((ProceedingJoinPoint) anyObject());
	}
}
