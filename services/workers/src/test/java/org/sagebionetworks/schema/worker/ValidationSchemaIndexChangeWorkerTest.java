package org.sagebionetworks.schema.worker;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

@ExtendWith(MockitoExtension.class)
public class ValidationSchemaIndexChangeWorkerTest {
	
	@Mock
	JsonSchemaManager mockSchemaManager;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@InjectMocks
	ValidationSchemaIndexChangeWorker worker;
	
	ChangeMessage message;
	String objectId;
	
	@BeforeEach
	public void before() {
		objectId = "1";
		message = new ChangeMessage();
		message.setObjectType(ObjectType.JSON_SCHEMA);
		message.setObjectId(objectId);
		message.setChangeType(ChangeType.UPDATE);
	}
	
	@Test
	public void testRun() throws Exception {
		worker.run(mockProgressCallback, message);
		verify(mockSchemaManager).createOrUpdateValidationSchemaIndex(objectId, ChangeType.UPDATE);
	}
	
	@Test
	public void testRunWithDifferentChangeType() throws Exception {
		// show that it sets changeType based on message
		message.setChangeType(ChangeType.CREATE);
		worker.run(mockProgressCallback, message);
		verify(mockSchemaManager).createOrUpdateValidationSchemaIndex(objectId, ChangeType.CREATE);
	}
}
