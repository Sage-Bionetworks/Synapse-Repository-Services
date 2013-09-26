package org.sagebionetworks.auth;


import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * These tests will require the UserManager to be pointed towards RDS rather than Crowd
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CrowdlessAuthenticationControllerAutowireTest {
	
	

}
