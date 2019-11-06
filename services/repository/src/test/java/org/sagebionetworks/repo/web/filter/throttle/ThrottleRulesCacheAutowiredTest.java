package org.sagebionetworks.repo.web.filter.throttle;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ThrottleRulesCacheAutowiredTest {
	@Autowired
	private ThrottleRulesCache throttleRulesCache;
	
	//test to make sure the scheduler is correctly wired to update the cache
	@Ignore //PLFM-4156
	@Test(timeout = 10 * 1000)
	public void testSchedulerUpdate(){
		while(true){
			if(throttleRulesCache.getLastUpdated() > 0){
				System.out.println("Cache was updated at " + throttleRulesCache.getLastUpdated() );
				return; //passed
			}
		}
	}
}
