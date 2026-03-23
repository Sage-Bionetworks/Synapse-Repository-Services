package org.sagebionetworks.principal.worker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class InactiveUsersWorkerIntegrationTest {

	@Autowired
	private UserStatusDao userStatusDao;
	
	@Autowired
	private UserManager	 userManager;
	
	@Autowired
	private Scheduler scheduler;
		
	private Trigger trigger;
	
	private UserInfo userInfo;
	
	@BeforeEach
	public void before() throws SchedulerException {
		trigger = scheduler.getTrigger(new TriggerKey("inactiveUsersWorkerTrigger"));
				
		UserInfo  adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userInfo = userManager.createOrGetTestUser(adminUserInfo, new NewUser().setUserName(UUID.randomUUID().toString()).setEmail(UUID.randomUUID().toString() + "@foo.org"));
		
		userStatusDao.setLastSeenOn(List.of(userInfo.getId()), Date.from(Instant.now().minus(390, ChronoUnit.DAYS)));
	}
	
	@Test
	public void testRun() throws Exception {
		
		scheduler.triggerJob(trigger.getJobKey(), trigger.getJobDataMap());
		
		TimeUtils.waitFor(60 * 1000, 1000L, () -> {
			return new Pair<Boolean, Void>(userStatusDao.isDisabled(userInfo.getId()), null);
		});
	}

	
}
