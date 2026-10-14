package org.sagebionetworks.recordset.worker;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.table.RecordSetIndexManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

@ExtendWith(MockitoExtension.class)
public class RecordSetIndexWorkerTest {

	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	@Mock
	private RecordSetIndexManager mockManager;
	@Mock
	private ProgressCallback mockProgressCallback;

	@InjectMocks
	private RecordSetIndexWorker worker;

	private ChangeMessage change;
	private IdAndVersion idAndVersion;

	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(RecordSetIndexWorker.class.getName())).thenReturn(mockLogger);
		// reinitialize so the logger stub is picked up
		worker = new RecordSetIndexWorker(mockManager, mockLoggerProvider);

		change = new ChangeMessage();
		change.setChangeNumber(99L);
		change.setObjectId("syn123");
		change.setObjectType(ObjectType.RECORDSET);
		change.setObjectVersion(2L);
		change.setChangeType(ChangeType.UPDATE);
		idAndVersion = IdAndVersion.newBuilder().setId(123L).setVersion(2L).build();
	}

	@Test
	public void testRunWithUpdate() throws RecoverableMessageException, Exception {
		// call under test
		worker.run(mockProgressCallback, change);
		verify(mockManager).createOrUpdateRecordSetIndex(idAndVersion, mockProgressCallback);
		verify(mockManager, never()).deleteRecordSetIndex(any());
	}

	@Test
	public void testRunWithCreate() throws RecoverableMessageException, Exception {
		change.setChangeType(ChangeType.CREATE);
		// call under test
		worker.run(mockProgressCallback, change);
		verify(mockManager).createOrUpdateRecordSetIndex(idAndVersion, mockProgressCallback);
		verify(mockManager, never()).deleteRecordSetIndex(any());
	}

	@Test
	public void testRunWithDelete() throws RecoverableMessageException, Exception {
		change.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockProgressCallback, change);

		verifyNoInteractions(mockManager);
	}

	@Test
	public void testRunIgnoresNonRecordSetMessages() throws RecoverableMessageException, Exception {
		change.setObjectType(ObjectType.ENTITY);
		// call under test
		worker.run(mockProgressCallback, change);
		verifyNoMoreInteractions(mockManager);
	}

	@Test
	public void testRunWrapsConnectionUnavailableAsRecoverable() throws RecoverableMessageException, Exception {
		TableIndexConnectionUnavailableException cause = new TableIndexConnectionUnavailableException("nope");
		doThrow(cause).when(mockManager).createOrUpdateRecordSetIndex(idAndVersion, mockProgressCallback);
		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> worker.run(mockProgressCallback, change));
		assertSame(cause, thrown.getCause());
	}

	@Test
	public void testRunWrapsLockUnavailableAsRecoverable() throws RecoverableMessageException, Exception {
		LockUnavilableException cause = new LockUnavilableException(LockType.Read, "k", "ctx");
		doThrow(cause).when(mockManager).createOrUpdateRecordSetIndex(idAndVersion, mockProgressCallback);
		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> worker.run(mockProgressCallback, change));
		assertSame(cause, thrown.getCause());
	}

	@Test
	public void testRunWrapsTableUnavailableAsRecoverable() throws RecoverableMessageException, Exception {
		TableUnavailableException cause = new TableUnavailableException(null);
		doThrow(cause).when(mockManager).createOrUpdateRecordSetIndex(idAndVersion, mockProgressCallback);
		// call under test
		RecoverableMessageException thrown = assertThrows(RecoverableMessageException.class,
				() -> worker.run(mockProgressCallback, change));
		assertSame(cause, thrown.getCause());
	}

	@Test
	public void testRunSwallowsGenericExceptions() throws RecoverableMessageException, Exception {
		doThrow(new RuntimeException("boom")).when(mockManager).createOrUpdateRecordSetIndex(idAndVersion,
				mockProgressCallback);
		// call under test (does not throw)
		worker.run(mockProgressCallback, change);
	}
}
