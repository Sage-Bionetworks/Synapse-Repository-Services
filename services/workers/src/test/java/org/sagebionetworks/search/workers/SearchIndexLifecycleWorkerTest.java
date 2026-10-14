package org.sagebionetworks.search.workers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.manager.search.SearchIndexLifecycleManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

@ExtendWith(MockitoExtension.class)
public class SearchIndexLifecycleWorkerTest {

	private static final String ENTITY_ID = "syn456";
	private static final Long USER_ID = 123L;

	@Mock
	private NodeDAO nodeDao;
	@Mock
	private SearchIndexLifecycleManager searchIndexLifecycleManager;
	@Mock
	private ProgressCallback progressCallback;

	private SearchIndexLifecycleWorker worker;

	@BeforeEach
	public void setUp() {
		worker = new SearchIndexLifecycleWorker(nodeDao, searchIndexLifecycleManager);
	}

	private ChangeMessage entityMessage(String id, ChangeType type) {
		ChangeMessage msg = new ChangeMessage();
		msg.setObjectType(ObjectType.ENTITY);
		msg.setObjectId(id);
		msg.setChangeType(type);
		msg.setUserId(USER_ID);
		return msg;
	}

	@Test
	public void testRunWithNonEntityObjectType() throws Exception {
		ChangeMessage msg = new ChangeMessage();
		msg.setObjectType(ObjectType.TABLE);
		msg.setObjectId(ENTITY_ID);
		msg.setChangeType(ChangeType.CREATE);

		// call under test
		worker.run(progressCallback, msg);

		verifyNoMoreInteractions(nodeDao);
		verifyNoMoreInteractions(searchIndexLifecycleManager);
	}

	private ChangeMessage searchIndexMessage(String id) {
		ChangeMessage msg = new ChangeMessage();
		msg.setObjectType(ObjectType.SEARCH_INDEX);
		msg.setObjectId(id);
		msg.setChangeType(ChangeType.UPDATE);
		return msg;
	}

	@Test
	public void testRunWithSearchIndexObjectType() throws Exception {
		// A source-dependency fan-out message: rebuild the search index if stale, no node type lookup.
		// call under test
		worker.run(progressCallback, searchIndexMessage(ENTITY_ID));

		verify(searchIndexLifecycleManager).rebuildIfStale(progressCallback, ENTITY_ID);
		verifyNoMoreInteractions(nodeDao);
	}

	@Test
	public void testRunWithSearchIndexObjectTypeRecoverableException() throws Exception {
		doThrow(new RecoverableMessageException("recoverable"))
				.when(searchIndexLifecycleManager).rebuildIfStale(progressCallback, ENTITY_ID);

		// call under test
		assertThrows(RecoverableMessageException.class, () ->
				worker.run(progressCallback, searchIndexMessage(ENTITY_ID)));
	}

	@Test
	public void testRunWithSearchIndexObjectTypeTableFailedIsSwallowed() throws Exception {
		doThrow(new TableFailedException(new TableStatus()))
				.when(searchIndexLifecycleManager).rebuildIfStale(progressCallback, ENTITY_ID);

		// call under test — the manager records FAILED; worker logs and moves on.
		worker.run(progressCallback, searchIndexMessage(ENTITY_ID));

		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithNonSearchIndexEntityType() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.file);

		// call under test
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE));

		verifyNoMoreInteractions(searchIndexLifecycleManager);
	}

	@Test
	public void testRunWithCreateChangeType() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);

		// call under test
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE));

		verify(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleUpdate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithUpdateChangeType() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);

		// call under test
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.UPDATE));

		verify(searchIndexLifecycleManager).handleUpdate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleCreate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithDeleteChangeType() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);

		// call under test
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.DELETE));

		verify(searchIndexLifecycleManager).handleDelete(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleCreate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleUpdate(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithNotFoundException() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenThrow(new NotFoundException("not found"));

		// call under test
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.UPDATE));

		verify(searchIndexLifecycleManager).handleDelete(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleCreate(progressCallback, ENTITY_ID);
		verify(searchIndexLifecycleManager, never()).handleUpdate(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithRecoverableException() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		doThrow(new RecoverableMessageException("recoverable"))
				.when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test
		assertThrows(RecoverableMessageException.class, () ->
				worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE)));

		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithTableUnavailableExceptionIsRecoverable() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		doThrow(new TableUnavailableException(new TableStatus()))
				.when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test
		assertThrows(RecoverableMessageException.class, () ->
				worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE)));

		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@Test
	public void testRunWithLockUnavailableExceptionIsRecoverable() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		doThrow(new LockUnavilableException(LockType.Read, "key", "context"))
				.when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test
		assertThrows(RecoverableMessageException.class, () ->
				worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE)));
	}

	@Test
	public void testRunWithTableFailedExceptionIsSwallowed() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		doThrow(new TableFailedException(new TableStatus()))
				.when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test — the manager records FAILED; worker logs and moves on.
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE));

		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}

	@ParameterizedTest
	@ValueSource(classes = {
			LockReleaseFailedException.class,
			CannotAcquireLockException.class,
			DeadlockLoserDataAccessException.class
	})
	public void testRunWithTransientLockExceptionIsRecoverable(Class<? extends RuntimeException> exceptionClass) throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		RuntimeException ex;
		if (exceptionClass == LockReleaseFailedException.class) {
			ex = new LockReleaseFailedException("lost lock");
		} else if (exceptionClass == CannotAcquireLockException.class) {
			ex = new CannotAcquireLockException("cannot acquire");
		} else {
			ex = new DeadlockLoserDataAccessException("deadlock", null);
		}
		doThrow(ex).when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test
		assertThrows(RecoverableMessageException.class, () ->
				worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE)));
	}

	@Test
	public void testRunWithUnknownRuntimeExceptionIsSwallowed() throws Exception {
		when(nodeDao.getNodeTypeById(ENTITY_ID)).thenReturn(EntityType.searchindex);
		doThrow(new RuntimeException("boom"))
				.when(searchIndexLifecycleManager).handleCreate(progressCallback, ENTITY_ID);

		// call under test — unknown runtime exceptions are logged and swallowed.
		worker.run(progressCallback, entityMessage(ENTITY_ID, ChangeType.CREATE));

		verify(searchIndexLifecycleManager, never()).handleDelete(progressCallback, ENTITY_ID);
	}
}
