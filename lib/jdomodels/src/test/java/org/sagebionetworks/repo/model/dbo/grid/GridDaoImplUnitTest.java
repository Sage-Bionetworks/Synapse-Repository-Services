package org.sagebionetworks.repo.model.dbo.grid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;

@ExtendWith(MockitoExtension.class)
public class GridDaoImplUnitTest {

	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	@Mock
	private TransactionSynchronizationProxy mockTransactionSynchronizationProxy;
	@Mock
	private GridDao mockSelf;
	@Captor
	private ArgumentCaptor<TransactionSynchronization> synchronizationCaptor;

	private GridDaoImpl dao;
	private String sessionId;
	private LogicalTimestamp patchId;
	private String s3Key;

	@BeforeEach
	public void before() {
		dao = new GridDaoImpl(mockIdGenerator, mockJdbcTemplate, mockTransactionSynchronizationProxy, mockSelf);
		sessionId = "session123";
		patchId = new LogicalTimestamp().setReplicaId(66538L).setSequenceNumber(42L);
		s3Key = "patch-key.json";
		when(mockIdGenerator.generateNewId(IdType.GRID_SESSION_ID)).thenReturn(987L);
	}

	@Test
	public void testSavePatchWithNewPatch() {
		when(mockJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

		// call under test
		assertTrue(dao.savePatch(sessionId, patchId, s3Key, 100L));

		verify(mockTransactionSynchronizationProxy).registerSynchronization(synchronizationCaptor.capture());
		// Nothing is marked inside the patch transaction
		verifyNoInteractions(mockSelf);

		synchronizationCaptor.getValue().afterCommit();

		verify(mockSelf).markSnapshotEligible(sessionId, patchId, s3Key);
	}

	@Test
	public void testSavePatchWithRollback() {
		when(mockJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

		// call under test
		assertTrue(dao.savePatch(sessionId, patchId, s3Key, 100L));

		verify(mockTransactionSynchronizationProxy).registerSynchronization(synchronizationCaptor.capture());
		synchronizationCaptor.getValue().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		verifyNoInteractions(mockSelf);
	}

	@Test
	public void testSavePatchWithDuplicatePatch() {
		when(mockJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

		// call under test
		assertFalse(dao.savePatch(sessionId, patchId, s3Key, 100L));

		verifyNoInteractions(mockTransactionSynchronizationProxy, mockSelf);
	}

	@Test
	public void testSavePatchWithMarkerFailure() {
		when(mockJdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
		doThrow(new DeadlockLoserDataAccessException("deadlock", null)).when(mockSelf)
				.markSnapshotEligible(sessionId, patchId, s3Key);

		// call under test
		assertTrue(dao.savePatch(sessionId, patchId, s3Key, 100L));

		verify(mockTransactionSynchronizationProxy).registerSynchronization(synchronizationCaptor.capture());
		// The failure is logged and swallowed, so it never reaches the committing caller
		synchronizationCaptor.getValue().afterCommit();

		verify(mockSelf).markSnapshotEligible(sessionId, patchId, s3Key);
	}
}
