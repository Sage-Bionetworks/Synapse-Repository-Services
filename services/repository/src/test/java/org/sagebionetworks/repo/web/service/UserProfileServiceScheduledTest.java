package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class UserProfileServiceScheduledTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	UserProfileService userProfileService;
	
	/**
	 * Check whether the cache has been built as scheduled.
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	@Test
	public void testPopulateCache() throws DatastoreException, NotFoundException, InterruptedException {
		// wait a bit for cache to be populated
		Thread.sleep(500L);
		assertNotNull("Cache has not been populated.", userProfileService.millisSinceLastCacheUpdate());
	}	

}
