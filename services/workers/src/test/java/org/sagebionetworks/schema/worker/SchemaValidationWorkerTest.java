package org.sagebionetworks.schema.worker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.schema.EntitySchemaValidator;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class SchemaValidationWorkerTest {

	@Mock
	private EntitySchemaValidator mockEntitySchemaManager;
	@Mock
	private WorkerLogger mockWorkerLogger;
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@InjectMocks
	private SchemaValidationWorker worker;

	String entityIdOne;
	String nonEntityId;
	String entityIdTwo;

	List<ChangeMessage> messages;

	@BeforeEach
	public void before() {
		entityIdOne = "syn123";
		nonEntityId = "not-an-entity-id";
		entityIdTwo = "syn456";

		ChangeMessage one = new ChangeMessage();
		one.setObjectType(ObjectType.ENTITY);
		one.setChangeType(ChangeType.CREATE);
		one.setObjectId(entityIdOne);

		ChangeMessage two = new ChangeMessage();
		two.setObjectType(ObjectType.ACTIVITY);
		two.setObjectId(nonEntityId);

		ChangeMessage three = new ChangeMessage();
		three.setObjectType(ObjectType.ENTITY);
		three.setChangeType(ChangeType.UPDATE);
		three.setObjectId(entityIdTwo);

		messages = Lists.newArrayList(one, two, three);
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {

		// call under test
		worker.run(mockProgressCallback, messages);
		verify(mockEntitySchemaManager).validateObject(entityIdOne);
		verify(mockEntitySchemaManager, never()).validateObject(nonEntityId);
		verify(mockEntitySchemaManager).validateObject(entityIdTwo);
		verifyZeroInteractions(mockWorkerLogger);

	}

	@Test
	public void testDeleteWithVersionChangeMessageIsIgnored() throws RecoverableMessageException, Exception {
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setObjectType(ObjectType.ENTITY);
		changeMessage.setChangeType(ChangeType.DELETE);
		changeMessage.setObjectVersion(1L);
		// call under test
		worker.run(mockProgressCallback, List.of(changeMessage));
		verifyZeroInteractions(mockEntitySchemaManager);

	}

	@Test
	public void testDeleteWithoutVersionChangeMessageIsProcessed() throws RecoverableMessageException, Exception {
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setObjectType(ObjectType.ENTITY);
		changeMessage.setChangeType(ChangeType.DELETE);
		changeMessage.setObjectVersion(null);
		changeMessage.setObjectId(entityIdOne);

		// call under test
		worker.run(mockProgressCallback, List.of(changeMessage));
		verify(mockEntitySchemaManager).validateObject(entityIdOne);
		verifyZeroInteractions(mockWorkerLogger);

	}

	@Test
	public void testRunWithNullChanges() throws RecoverableMessageException, Exception {
		messages = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, messages);
		});
	}

	@Test
	public void testRunWithFailures() throws RecoverableMessageException, Exception {
		IllegalStateException someException = new IllegalStateException("something went wrong");
		doThrow(someException).when(mockEntitySchemaManager).validateObject(entityIdOne);
		// call under test
		worker.run(mockProgressCallback, messages);
		verify(mockEntitySchemaManager).validateObject(entityIdOne);
		verify(mockEntitySchemaManager, never()).validateObject(nonEntityId);
		verify(mockEntitySchemaManager).validateObject(entityIdTwo);
		// The error should get logged
		boolean willRetry = false;
		verify(mockWorkerLogger).logWorkerFailure(SchemaValidationWorker.class, messages.get(0), someException,
				willRetry);

	}
}
