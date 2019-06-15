package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableTransactionDaoImplTest {

	@Autowired
	TableTransactionDao tableTransactionDao;
	
	@Autowired
	TransactionTemplate readCommitedTransactionTemplate;
	
	Long userId;
	
	String tableId;
	long tableIdLong;
	
	@Before
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(userId);
		tableId = "syn123";
		tableIdLong = KeyFactory.stringToKey(tableId);
		tableTransactionDao.deleteTable(tableId);
	}
	
	
	@Test
	public void testCreateGetDelete() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		TableTransaction trx = tableTransactionDao.getTransaction(transactionId);
		assertNotNull(trx);
		assertEquals(transactionId, trx.getTransactionId());
		assertEquals(tableId, trx.getTableId());
		assertEquals(userId, trx.getStartedBy());
		assertNotNull(trx.startedOn);
		assertEquals(1, tableTransactionDao.deleteTable(tableId));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartTransactionNullTableId() {
		tableId = null;
		// call under test
		tableTransactionDao.startTransaction(tableId, userId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStartTransactionNullUserd() {
		userId = null;
		// call under test
		tableTransactionDao.startTransaction(tableId, userId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetNotFound() {
		// call under test
		tableTransactionDao.getTransaction(-1L);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetNullId() {
		Long id = null;
		// call under test
		tableTransactionDao.getTransaction(id);
	}
	
	@Test
	public void testMultipleTableDelete() {
		// start clean
		String tableOneId = "syn456";
		tableTransactionDao.deleteTable(tableOneId);
		String tableTwoId = "syn789";
		tableTransactionDao.deleteTable(tableTwoId);
		
		Long oneOne = tableTransactionDao.startTransaction(tableOneId, userId);
		Long oneTwo = tableTransactionDao.startTransaction(tableOneId, userId);
		assertTrue(oneTwo == oneOne + 1);
		Long twoOne = tableTransactionDao.startTransaction(tableTwoId, userId);
		assertTrue(twoOne == oneTwo + 1);
		// delete both from one first.
		assertEquals(2, tableTransactionDao.deleteTable(tableOneId));
		// should still be able to get two
		assertNotNull(tableTransactionDao.getTransaction(twoOne));
		assertEquals(1, tableTransactionDao.deleteTable(tableTwoId));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteNullTableId() {
		tableId = null;
		// call under test
		tableTransactionDao.deleteTable(tableId);
	}
	
	@Test
	public void testGetTableIdWithLockNoTransaction() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		try {
			// call under test
			tableTransactionDao.getTableIdWithLock(transactionId);
			fail();
		} catch (IllegalTransactionStateException e) {
			// expected
		}
	}
	
	@Test
	public void testGetTableIdWithLock() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		// call under test
		long resultTableId = getTableIdWithLock(transactionId);
		assertEquals(tableIdLong, resultTableId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetTableIdWithLockNotFound() {
		Long transactionId = -1L;
		// call under test
		getTableIdWithLock(transactionId);
	}
	
	@Test
	public void testlinkTransactionToVersionNoTransaction() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		long version = 15L;
		try {
			tableTransactionDao.linkTransactionToVersion(transactionId, version);
			fail();
		} catch (IllegalTransactionStateException e) {
			// expected
		}
	}
	
	@Test
	public void testlinkTransactionToVersion() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		long version = 15L;
		// call under test
		linkTransactionToVersion(transactionId, version);
		Optional<Long> lookupTransaction = tableTransactionDao.getTransactionForVersion(tableId, version);
		assertNotNull(lookupTransaction);
		assertTrue(lookupTransaction.isPresent());
		assertEquals(transactionId, lookupTransaction.get());
	}
	
	@Test
	public void testGetTransactionForVersionDoesNotExist() {
		// no transaction linked to this table version.
		long version = 15L;
		// call under test
		Optional<Long> lookupTransaction = tableTransactionDao.getTransactionForVersion(tableId, version);
		assertNotNull(lookupTransaction);
		assertFalse(lookupTransaction.isPresent());
	}
	
	@Test
	public void testUpdateEtag() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		TableTransaction startTrans = tableTransactionDao.getTransaction(transactionId);
		assertNotNull(startTrans);
		assertNotNull(startTrans.getEtag());
		// call under test
		String newEtag = updateTransactionEtag(transactionId);
		assertNotNull(newEtag);
		TableTransaction afterUpdate = tableTransactionDao.getTransaction(transactionId);
		assertNotNull(afterUpdate);
		assertNotNull(afterUpdate.getEtag());
		assertNotEquals(afterUpdate.getEtag(), startTrans.getEtag());
		assertEquals(newEtag, afterUpdate.getEtag());
	}
	
	@Test
	public void testUpdateEtagNoTransaction() {
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		try {
			// call under test
			tableTransactionDao.updateTransactionEtag(transactionId);
			fail();
		} catch (IllegalTransactionStateException e) {
			// expected.
		}
	}
	
	/**
	 * Helper to call updateTransactionEtag from within a database transaction.
	 * 
	 * @param transactionId
	 * @return
	 */
	String updateTransactionEtag(long transactionId) {
		return readCommitedTransactionTemplate.execute((TransactionStatus status) -> {
			return tableTransactionDao.updateTransactionEtag(transactionId);
		});
	}

	/**
	 * Helper to call linkTransactionToVersion within a database transaction.
	 * @param transactionId
	 * @param version
	 */
	void linkTransactionToVersion(long transactionId, long version) {
		readCommitedTransactionTemplate.execute((TransactionStatus status) -> {
			tableTransactionDao.linkTransactionToVersion(transactionId, version);
			return null;
		});
	}
	
	/**
	 * Helper to call getTableIdWithLock within a transaction.
	 * 
	 * @param transactionId
	 * @return
	 */
	Long getTableIdWithLock(long transactionId) {
		return readCommitedTransactionTemplate.execute((TransactionStatus status) -> {
			return tableTransactionDao.getTableIdWithLock(transactionId);
		});
	}
}
