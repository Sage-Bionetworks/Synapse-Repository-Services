package org.sagebionetworks.table.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class TableCurrentCacheWorkerTest {

	ProgressCallback<ChangeMessage> mockProgressCallback;
	TableRowManager mockTableRowManager;
	StackConfiguration mockConfiguration;
	NodeInheritanceManager mockNodeInheritanceManager;
	TableCurrentCacheWorker worker;
	ChangeMessage one;
	ChangeMessage two;

	@Before
	public void before() throws LockUnavilableException, InterruptedException, Exception {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockTableRowManager = Mockito.mock(TableRowManager.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockNodeInheritanceManager = Mockito.mock(NodeInheritanceManager.class);
		// Turn on the feature by default
		when(mockConfiguration.getTableEnabled()).thenReturn(true);
		when(mockNodeInheritanceManager.isNodeInTrash(anyString())).thenReturn(false);
		worker = new TableCurrentCacheWorker();
		ReflectionTestUtils.setField(worker, "tableRowManager", mockTableRowManager);
		ReflectionTestUtils.setField(worker, "nodeInheritanceManager", mockNodeInheritanceManager);
		ReflectionTestUtils.setField(worker, "stackConfiguration", mockConfiguration);
		
		one = new ChangeMessage();
		one.setChangeType(ChangeType.CREATE);
		one.setObjectId("123");
		one.setObjectType(ObjectType.TABLE);
		one.setObjectEtag("etag");
		
		two = new ChangeMessage();
		two.setChangeType(ChangeType.CREATE);
		two.setObjectId("456");
		two.setObjectType(ObjectType.TABLE);
		two.setObjectEtag("etag");
	}


	/**
	 * Non-table messages should simply be returned so they can be removed from the queue.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNonTableMessages() throws Exception {
		one.setObjectType(ObjectType.ACTIVITY);
		two.setObjectType(ObjectType.ACTIVITY);
		// call under test
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * When the table feature is disabled all messages should be removed from the queue and no other methods should be
	 * called.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFeatureDisabled() throws Exception {
		// Disable the feature
		when(mockConfiguration.getTableEnabled()).thenReturn(false);
		two.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * A delete message should result in the table index also being deleted.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTableDelete() throws Exception {
		String tableId = "456";
		two.setChangeType(ChangeType.DELETE);
		// call under test
		worker.run(mockProgressCallback, two);
		verify(mockTableRowManager).removeCaches(tableId);
	}

	@Test
	public void testTableInTrash() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		when(mockNodeInheritanceManager.isNodeInTrash(tableId)).thenReturn(true);
		two.setChangeType(ChangeType.UPDATE);
		// call under test
		worker.run(mockProgressCallback, two);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	@Test
	public void testTableNotExists() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		when(mockNodeInheritanceManager.isNodeInTrash(tableId)).thenThrow(new NotFoundException("dummy"));
		two.setChangeType(ChangeType.UPDATE);
		// call under test
		worker.run(mockProgressCallback, two);
		verifyNoMoreInteractions(mockTableRowManager);
	}

	/**
	 * When everything works well, the message should be removed from the queue and the table status should be set to
	 * AVAILABLE.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHappyCase() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
		verify(mockTableRowManager).updateLatestVersionCache(eq(tableId), any(org.sagebionetworks.util.ProgressCallback.class));
	}

	/**
	 * When an unknown exception is thrown the table message should be retried
	 * 
	 * @throws Exception
	 */
	@Test(expected = IOException.class)
	public void testFailing() throws Exception {
		String tableId = "456";
		String resetToken = "reset-token";
		TableStatus status = new TableStatus();
		status.setResetToken(resetToken);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		// This should trigger a failure
		doThrow(new IOException("mock")).when(mockTableRowManager).updateLatestVersionCache(eq(tableId), any(org.sagebionetworks.util.ProgressCallback.class));
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken);
		// call under test
		worker.run(mockProgressCallback, two);
	}

	/**
	 * When the reset-tokens do not match, the table has been updated since the message was sent. When this occurs the
	 * status cannot be set to AVAILABLE
	 * 
	 * @throws Exception
	 */
	@Test
	public void testResetTokenDoesNotMatch() throws Exception {
		String tableId = "456";
		String resetToken1 = "reset-token1";
		String resetToken2 = "reset-token2";
		TableStatus status = new TableStatus();
		// Set the current token to be different than the token in the message
		status.setResetToken(resetToken2);
		when(mockTableRowManager.getTableStatusOrCreateIfNotExists(tableId)).thenReturn(status);
		two.setChangeType(ChangeType.UPDATE);
		two.setObjectEtag(resetToken1);
		// call under test
		worker.run(mockProgressCallback, two);
		verify(mockTableRowManager).getTableStatusOrCreateIfNotExists(tableId);
		verifyNoMoreInteractions(mockTableRowManager);
	}
}
