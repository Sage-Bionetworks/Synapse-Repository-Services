package org.sagebionetworks.message.workers;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.BroadcastMessageManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

public class BroadcastMessageWorkerTest {

	@Mock
	private BroadcastMessageManager mockBroadcastManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private ProgressCallback<ChangeMessage> mockCallback;
	private BroadcastMessageWorker worker;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		worker = new BroadcastMessageWorker();
		ReflectionTestUtils.setField(worker, "broadcastManager", mockBroadcastManager);
		ReflectionTestUtils.setField(worker, "userManager", mockUserManager);
	}

	@Test
	public void test() throws RecoverableMessageException, Exception {
		ChangeMessage fakeMessage = new ChangeMessage();
		fakeMessage.setChangeType(ChangeType.CREATE);
		fakeMessage.setObjectType(ObjectType.THREAD);
		worker.run(mockCallback, fakeMessage);
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockBroadcastManager).broadcastMessage(any(UserInfo.class), eq(mockCallback), eq(fakeMessage));
	}
}
