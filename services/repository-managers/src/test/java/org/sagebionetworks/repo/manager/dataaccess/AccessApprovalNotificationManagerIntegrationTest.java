package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessApprovalNotificationManagerIntegrationTest {

	@Autowired
	private AccessApprovalNotificationManager manager;
	
	@Autowired
	private FeatureStatusDao featureTesting;
	
	@BeforeEach
	public void before() {
		featureTesting.setFeatureEnabled(Feature.DATA_ACCESS_RENEWALS, true);
	}
	
	@Test
	public void processAccessApprovalChangeWithNonExistingAccessApproval() throws RecoverableMessageException {
		ChangeMessage message = new ChangeMessage();
		
		message.setChangeNumber(12345L);
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(ObjectType.ACCESS_APPROVAL);
		message.setTimestamp(new Date());
		message.setObjectId("1");
		
		assertThrows(NotFoundException.class, () -> {			
			manager.processAccessApprovalChange(message);
		});
	}
	
}
