package org.sagebionetworks.table.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.FileViewManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;

public class FileViewWorkerTest {

	@Mock
	FileViewManager tableViewManager;
	@Mock
	TableManagerSupport tableManagerSupport;
	@Mock
	TableIndexConnectionFactory connectionFactory;
	@Mock
	TableIndexManager indexManager;
	@Mock
	ProgressCallback<ChangeMessage> outerCallback;
	@Mock
	ProgressCallback<ChangeMessage> innerCallback;

	FileViewWorker worker;

	String tableId;
	ChangeMessage change;
	String token;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);

		worker = new FileViewWorker();
		ReflectionTestUtils.setField(worker, "tableViewManager",
				tableViewManager);
		ReflectionTestUtils.setField(worker, "tableManagerSupport",
				tableManagerSupport);
		ReflectionTestUtils.setField(worker, "connectionFactory",
				connectionFactory);

		tableId = "123";
		change = new ChangeMessage();
		change.setChangeNumber(99L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId(tableId);
		change.setObjectType(ObjectType.FILE_VIEW);
		
		token = "statusToken";
		
		// setup default responses
	
		when(tableManagerSupport.startTableProcessing(tableId)).thenReturn(token);
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(false);

		when(connectionFactory.connectToTableIndex(tableId)).thenReturn(
				indexManager);

		// By default the lock should proceed with the callback.
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				ProgressingCallable<Void, ChangeMessage> callable = (ProgressingCallable<Void, ChangeMessage>) invocation
						.getArguments()[3];
				// pass it along
				if(callable != null){
					callable.call(innerCallback);
				}
				return null;
			}
		}).when(tableManagerSupport).tryRunWithTableExclusiveLock(
				any(ProgressCallback.class), anyString(), anyInt(),
				any(ProgressingCallable.class));

	}

	@Test
	public void testRunNonFileViewChagne() throws Exception {
		// this worker should not respond to TABLE changes.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		worker.run(outerCallback, change);
		verify(connectionFactory, never()).connectToTableIndex(anyString());
	}

	@Test
	public void testRunDelete() throws Exception {
		change.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(outerCallback, change);
		verify(indexManager).deleteTableIndex();
	}

	@Test(expected = RecoverableMessageException.class)
	public void testRunConnectionUnavailable() throws Exception {
		// simulate no connection
		when(connectionFactory.connectToTableIndex(tableId)).thenThrow(
				new TableIndexConnectionUnavailableException("No connection"));
		// call under test
		worker.run(outerCallback, change);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RecoverableMessageException.class)
	public void testRunLockUnavilableException() throws Exception {
		// setup no lock
		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new LockUnavilableException("No lock for you"));
		// call under test
		worker.run(outerCallback, change);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = RecoverableMessageException.class)
	public void testRunLownerRecoverableMessageException() throws Exception {
		// If this exception is caught it must be re-thrown.
		when(
				tableManagerSupport.tryRunWithTableExclusiveLock(
						any(ProgressCallback.class), anyString(), anyInt(),
						any(ProgressingCallable.class))).thenThrow(
				new RecoverableMessageException("Should get re-thrown"));
		// call under test
		worker.run(outerCallback, change);
	}
	
	@Test
	public void testRunIsIndexSynchronizedTrue() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(true);
		// call under test
		worker.run(outerCallback, change);
		// progress should not start for this case
		verify(tableManagerSupport, never()).startTableProcessing(tableId);
	}
	
	@Test
	public void testRunIsIndexSynchronizedFalse() throws Exception{
		// Setup the synched state
		when(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)).thenReturn(false);
		// call under test
		worker.run(outerCallback, change);
		// progress should start for this case
		verify(tableManagerSupport).startTableProcessing(tableId);
	}

}
