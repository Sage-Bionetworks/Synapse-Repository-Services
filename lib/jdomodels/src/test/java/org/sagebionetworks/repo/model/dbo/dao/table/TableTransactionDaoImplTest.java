package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class TableTransactionDaoImplTest {

	@Autowired
	TableTransactionDao tableTransactionDao;
	
	Long userId;
	
	@Before
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		assertNotNull(userId);	
	}
	
	
	@Test
	public void testCreateAndGet() {
		String tableId = "syn123";
		Long transactionId = tableTransactionDao.startTransaction(tableId, userId);
		TableTransaction trx = tableTransactionDao.getTransaction(transactionId);
		assertNotNull(trx);
		assertEquals(transactionId, trx.getTransactionNumber());
		assertEquals(tableId, trx.getTableId());
		assertEquals(userId, trx.getStartedBy());
		assertNotNull(trx.startedOn);
	}
}
