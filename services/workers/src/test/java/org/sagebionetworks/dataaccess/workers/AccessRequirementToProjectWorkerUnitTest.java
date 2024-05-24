package org.sagebionetworks.dataaccess.workers;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementToProjectWorkerUnitTest {

	@Mock
	private AccessRequirementManager mockManager;
	
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@InjectMocks
	private AccessRequirementToProjectWorker worker;

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		List<ChangeMessage> messages = Lists.newArrayList(
				new ChangeMessage().setObjectId("1"),
				new ChangeMessage().setObjectType(ObjectType.ACCESS_CONTROL_LIST).setObjectId("2").setChangeType(ChangeType.CREATE),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setObjectId("3").setChangeType(ChangeType.DELETE),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setObjectId("4").setChangeType(ChangeType.CREATE),
				new ChangeMessage().setObjectType(ObjectType.ENTITY).setObjectId("5").setChangeType(ChangeType.UPDATE)
		);
		// call under test
		worker.run(mockProgressCallback, messages);
		List<String> expectedIds = Lists.newArrayList("4","5");
		verify(mockManager).mapAccessRequirementsToProject(expectedIds);
	}
}
