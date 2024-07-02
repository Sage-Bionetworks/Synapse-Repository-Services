package org.sagebionetworks.auth.workers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.auth.OAuthAccessTokenDao;
import org.sagebionetworks.repo.model.dbo.auth.OIDCAccessTokenData;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ExpiredAccessTokenWorkerIntegrationTest {
	
	@Autowired
	private OAuthAccessTokenDao accessTokenDao;
	
	@Autowired
	private Scheduler scheduler;
	
	private Trigger trigger;

	@BeforeEach
	public void before() throws SchedulerException {
		trigger = scheduler.getTrigger(new TriggerKey("expiredAccessTokensWorkerTrigger"));
	}
	
	@Test
	public void testRun() throws Exception {
		String tokenId = UUID.randomUUID().toString();
		
		accessTokenDao.storeAccessTokenRecord(new OIDCAccessTokenData()
			.setClientId(Long.valueOf(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID))
			.setPrincipalId(123L)
			.setTokenId(tokenId)
			.setCreatedOn(new Date())
			.setExpiresOn(Date.from(Instant.now().minus(2, ChronoUnit.DAYS)))
		);
		
		assertTrue(accessTokenDao.doesAccessTokenRecordExist(tokenId));
		
		scheduler.triggerJob(trigger.getJobKey(), trigger.getJobDataMap());
		
		TimeUtils.waitFor(60 * 1000, 1000L, () -> {
			return new Pair<Boolean, Void>(!accessTokenDao.doesAccessTokenRecordExist(tokenId), null);
		});
	}

}
