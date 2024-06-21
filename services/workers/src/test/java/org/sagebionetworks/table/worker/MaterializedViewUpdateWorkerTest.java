package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
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
public class MaterializedViewUpdateWorkerTest {
	
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	@Mock
	private MaterializedViewManager mockMaterializedViewManager;
	@Mock
	private ProgressCallback mockProgressCallback;;

	MaterializedViewUpdateWorker worker;

	String tableId;
	IdAndVersion idAndVersion;
	Long tableViewIdLong;
	ChangeMessage change;

	@BeforeEach
	public void beforeEach() {
		String tableId = "syn123";
		change = new ChangeMessage();
		change.setChangeNumber(99L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId(tableId);
		change.setObjectType(ObjectType.MATERIALIZED_VIEW);

		idAndVersion = IdAndVersion.parse(tableId);

		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker = new MaterializedViewUpdateWorker(mockMaterializedViewManager, mockLoggerProvider);
	}

	@Test
	public void testRunCreate() throws Exception {
		// call under test
		worker.run(mockProgressCallback, change);
		verify(mockMaterializedViewManager).createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		verify(mockMaterializedViewManager, never()).deleteViewIndex(any());
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunCreateWithVersion() throws Exception {
		String tableId = "syn123";
		long version = 44;
		idAndVersion = IdAndVersion.parse(tableId + "." + version);
		change.setObjectId(tableId);
		change.setObjectVersion(version);
		// call under test
		worker.run(mockProgressCallback, change);
		verify(mockMaterializedViewManager).createOrUpdateViewIndex(mockProgressCallback, idAndVersion);
		verify(mockMaterializedViewManager, never()).deleteViewIndex(any());
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunNotView() throws Exception {
		change.setObjectType(ObjectType.ACTIVITY);
		// call under test
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockMaterializedViewManager);
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunTableIndexConnectionUnavailableException() throws Exception {
		TableIndexConnectionUnavailableException exception = new TableIndexConnectionUnavailableException(
				"no connection");
		doThrow(exception).when(mockMaterializedViewManager).createOrUpdateViewIndex(any(), any());
		Throwable cause = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, change);
		}).getCause();
		assertEquals(cause, exception);
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunTableUnavailableException() throws Exception {
		TableUnavailableException exception = new TableUnavailableException(null);
		doThrow(exception).when(mockMaterializedViewManager).createOrUpdateViewIndex(any(),any());
		Throwable cause = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, change);
		}).getCause();
		assertEquals(cause, exception);
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunLockUnavilableException() throws Exception {
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "key", "context");
		doThrow(exception).when(mockMaterializedViewManager).createOrUpdateViewIndex(any(),any());
		Throwable cause = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, change);
		}).getCause();
		assertEquals(cause, exception);
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunRecoverableMessageException() throws Exception {
		RecoverableMessageException exception = new RecoverableMessageException("no lock");
		doThrow(exception).when(mockMaterializedViewManager).createOrUpdateViewIndex(any(),any());
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockProgressCallback, change);
		});
		assertEquals(result, exception);
		verifyZeroInteractions(mockLogger);
	}

	@Test
	public void testRunUnknownException() throws Exception {
		IllegalArgumentException exception = new IllegalArgumentException("no lock");
		doThrow(exception).when(mockMaterializedViewManager).createOrUpdateViewIndex(any(),any());
		// call under test
		worker.run(mockProgressCallback, change);
		verify(mockLogger).error("Failed to build materialized view index: ", exception);
	}
}
