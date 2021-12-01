package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

@ExtendWith(MockitoExtension.class)
public class TableTransactionManagerTest {

	@Mock
	private TableTransactionDao mockTransactionDao;
	
	@Mock
	private TableRowTruthDAO mockTruthDao;
	
	@Mock
	private TableManagerSupport mockManagerSupport;
	
	@Mock
	private TableTransactionContext mockTxContext;
	
	@InjectMocks
	private TableTransactionManagerImpl manager;

	private UserInfo user;
	private String tableId;
	private IdAndVersion idAndVersion;
	private long transactionId;
	
	@BeforeEach
	public void before() {
		user = new UserInfo(false, 1L);
		tableId = "123";
		idAndVersion = IdAndVersion.parse(tableId);
		transactionId = 456L;
	}

	@Test
	public void testExecuteInTransaction() {

		when(mockTransactionDao.startTransaction(any(), any())).thenReturn(transactionId);
		
		// Call under test
		long result = manager.executeInTransaction(user, tableId, txContext -> txContext.getTransactionId());

		assertEquals(transactionId, result);
		
		verify(mockTransactionDao).startTransaction(tableId, user.getId());
		verify(mockManagerSupport).touchTable(user, tableId);
		verify(mockManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}
	
	@Test
	public void testExecuteInTransactionMultipleGets() {

		when(mockTransactionDao.startTransaction(any(), any())).thenReturn(transactionId);
		
		// Call under test
		long result = manager.executeInTransaction(user, tableId, txContext -> {
			txContext.getTransactionId();
			txContext.getTransactionId();
			return txContext.getTransactionId();
		});

		assertEquals(transactionId, result);
		
		verify(mockTransactionDao).startTransaction(tableId, user.getId());
		verify(mockManagerSupport).touchTable(user, tableId);
		verify(mockManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
		
	}
	
	@Test
	public void testExecuteInTransactionWithNoStart() {
	
		// Call under test
		String result = manager.executeInTransaction(user, tableId, txContext -> "nothing");

		assertEquals("nothing", result);
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
		
	}
	
	@Test
	public void testExecuteInTransactionWithNoUser() {

		user = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.executeInTransaction(user, tableId, txContext -> txContext.getTransactionId());
		}).getMessage();
		
		assertEquals("The user is required.", message);
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
	}
	
	@Test
	public void testExecuteInTransactionWithNoTableId() {

		tableId = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.executeInTransaction(user, tableId, txContext -> txContext.getTransactionId());
		}).getMessage();
		
		assertEquals("The tableId is required.", message);
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
	}
	
	@Test
	public void testExecuteInTransactionWithNoFunction() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.executeInTransaction(user, tableId, null);
		}).getMessage();
		
		assertEquals("The function to execute is required.", message);
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
	}
		
	@Test
	public void testLinkVersionToLatestTransaction() {
		
		idAndVersion = IdAndVersion.parse("123.1");
		
		when(mockTruthDao.getLastTransactionId(any())).thenReturn(Optional.of(transactionId));
		when(mockTransactionDao.getTableIdWithLock(anyLong())).thenReturn(idAndVersion.getId());
	
		// Call under test
		manager.linkVersionToLatestTransaction(idAndVersion);
		
		verify(mockTransactionDao).linkTransactionToVersion(transactionId, idAndVersion.getVersion().get());
		verify(mockTransactionDao).updateTransactionEtag(transactionId);
		verify(mockManagerSupport).setTableToProcessingAndTriggerUpdate(idAndVersion);
	}
	
	@Test
	public void testLinkVersionToLatestTransactionWithNoTransaction() {
		
		idAndVersion = IdAndVersion.parse("123.1");
		
		when(mockTruthDao.getLastTransactionId(any())).thenReturn(Optional.empty());
	
		assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.linkVersionToLatestTransaction(idAndVersion);
		});
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
	}
		
	@Test
	public void testLinkVersionToLatestTransactionWithNoId() {
		
		idAndVersion = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToLatestTransaction(idAndVersion);
		}).getMessage();
		
		assertEquals("The tableId is required.", message);
		
		verifyZeroInteractions(mockTransactionDao);
		verifyZeroInteractions(mockManagerSupport);
	}
	
}
