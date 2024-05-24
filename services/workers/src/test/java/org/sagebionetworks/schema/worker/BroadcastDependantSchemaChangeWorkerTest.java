package org.sagebionetworks.schema.worker;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class BroadcastDependantSchemaChangeWorkerTest {
	
	@Mock
	JsonSchemaManager mockSchemaManager;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@InjectMocks
	BroadcastDependantSchemaChangeWorker worker;
	
	ChangeMessage message;
	String objectId;
	
	@BeforeEach
	public void before() {
		objectId = "1";
		message = new ChangeMessage();
		message.setObjectType(ObjectType.JSON_SCHEMA_DEPENDANT);
		message.setObjectId(objectId);
		message.setChangeType(ChangeType.UPDATE);
	}
	
	@Test
	public void testRun() throws Exception {
		worker.run(mockProgressCallback, message);
		verify(mockSchemaManager).sendUpdateNotificationsForDependantSchemas(objectId);
	}
}
