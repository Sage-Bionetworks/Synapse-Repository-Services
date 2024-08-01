package org.sagebionetworks.repo.manager.webhook;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.util.Clock;

@ExtendWith(MockitoExtension.class)
public class WebhookManagerUnitTest {

	@Mock
	private WebhookDao mockWebhookDao;

	@Mock
	private AccessControlListDAO mockAclDao;

	@Mock
	private UserManager mockUserManager;

	@Mock
	private Clock mockClock;

	@Mock
	private TransactionalMessenger mockTransactionalMessenger;

	@InjectMocks
	private WebhookManagerImpl webhookManager;


}
