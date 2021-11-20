package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
	}
	
	@Test
	public void testExecuteInTransactionWithNoTableId() {

		tableId = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.executeInTransaction(user, tableId, txContext -> txContext.getTransactionId());
		}).getMessage();
		
		assertEquals("The tableId is required.", message);
	}
	
	@Test
	public void testExecuteInTransactionWithNoFunction() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.executeInTransaction(user, tableId, null);
		}).getMessage();
		
		assertEquals("The function to execute is required.", message);
	}
	
	@Test
	public void testGetLastTransactionContext() {
		when(mockTruthDao.getLastTransactionId(any())).thenReturn(Optional.of(transactionId));
	
		// Call under test
		Optional<TableTransactionContext> result = manager.getLastTransactionContext(tableId);
		
		assertTrue(result.isPresent());
		assertEquals(transactionId, result.get().getTransactionId());
		
		verify(mockTruthDao).getLastTransactionId(tableId);
	}
	
	@Test
	public void testGetLastTransactionContextWithNoTable() {
		
		tableId = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getLastTransactionContext(tableId);
		}).getMessage();
		
		assertEquals("The tableId is required.", message);
		
	}
	
	@Test
	public void testLinkTransactionToVersion() {
		
		idAndVersion = IdAndVersion.parse("123.1");
		
		when(mockTxContext.getTransactionId()).thenReturn(transactionId);
		when(mockTxContext.isTransactionStarted()).thenReturn(true);
		when(mockTransactionDao.getTableIdWithLock(anyLong())).thenReturn(idAndVersion.getId());
	
		// Call under test
		manager.linkVersionToTransaction(mockTxContext, idAndVersion);
		
		verify(mockTransactionDao).linkTransactionToVersion(transactionId, idAndVersion.getVersion().get());
		verify(mockTransactionDao).updateTransactionEtag(transactionId);
	}
	
	@Test
	public void testLinkTransactionToVersionWithNoContext() {
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToTransaction(null, idAndVersion);
		}).getMessage();
		
		assertEquals("The transaction context is required.", message);
	}
	
	@Test
	public void testLinkTransactionToVersionWithNoId() {
		
		idAndVersion = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToTransaction(mockTxContext, idAndVersion);
		}).getMessage();
		
		assertEquals("The tableId is required.", message);
	}
	
	@Test
	public void testLinkTransactionToVersionWithTransactionNotStarted() {
		
		when(mockTxContext.isTransactionStarted()).thenReturn(false);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToTransaction(mockTxContext, idAndVersion);
		}).getMessage();
		
		assertEquals("The transaction was not started.", message);
	}
	
	@Test
	public void testLinkTransactionToVersionWithNoVersion() {
		when(mockTxContext.isTransactionStarted()).thenReturn(true);
		
		idAndVersion = IdAndVersion.parse("123");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToTransaction(mockTxContext, idAndVersion);
		}).getMessage();
		
		assertEquals("The table must have a version.", message);
	}
	
	@Test
	public void testLinkTransactionToVersionWithWrongTable() {
		
		when(mockTxContext.isTransactionStarted()).thenReturn(true);
		when(mockTxContext.getTransactionId()).thenReturn(transactionId);
		when(mockTransactionDao.getTableIdWithLock(anyLong())).thenReturn(1234546L);
		
		idAndVersion = IdAndVersion.parse("123.2");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.linkVersionToTransaction(mockTxContext, idAndVersion);
		}).getMessage();
		
		assertEquals("Transaction: " + transactionId + " is not associated with table: " + tableId, message);
		
		verify(mockTransactionDao).getTableIdWithLock(transactionId);
	}
}
