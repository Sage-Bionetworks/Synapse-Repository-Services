package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableTransactionDaoImplTest {

	@Autowired
	TableTransactionDao tableTransactionDao;
	
	Long userId;
	
	String tableId;
	
	@Before
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(userId);
		tableId = "syn123";
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
}
