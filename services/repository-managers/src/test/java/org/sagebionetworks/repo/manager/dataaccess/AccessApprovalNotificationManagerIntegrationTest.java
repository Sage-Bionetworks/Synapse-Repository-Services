package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
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
	private UserManager userManager;
	
	private UserInfo admin;
	
	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()); 
	}
	
	@Test
	public void processAccessApprovalChangeMessageWithNonExistingAccessApproval() throws RecoverableMessageException {
		ChangeMessage message = new ChangeMessage();
		
		message.setChangeNumber(12345L);
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectType(ObjectType.ACCESS_APPROVAL);
		message.setTimestamp(new Date());
		message.setObjectId("1");
		
		assertThrows(NotFoundException.class, () -> {			
			manager.processAccessApprovalChangeMessage(admin, message);
		});
	}
	
}
