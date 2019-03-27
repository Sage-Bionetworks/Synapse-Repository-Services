package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.dbo.auth.DBOAuthenticationReceiptDAOImpl.EXPIRATION_PERIOD;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.auth.AuthenticationReceiptDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAuthenticationReceiptDAOImplTest {

	@Autowired
	private AuthenticationReceiptDAO authReceiptDao;

	private Long userId;
	private String receipt;

	@Before
	public void before() {
		userId = 1L;
		receipt = "receipt";
	}

	@After
	public void after() {
		authReceiptDao.truncateAll();
	}

	@Test
	public void testReceiptInvalid() {
		assertFalse(authReceiptDao.isValidReceipt(userId, receipt));
	}

	@Test
	public void testIsValidReceipt_expired() throws InterruptedException {
		//expire after 1 millisecond
		receipt = authReceiptDao.createNewReceipt(userId, 1);
		Thread.sleep(3);
		assertFalse(authReceiptDao.isValidReceipt(userId, receipt));
	}

	@Test
	public void testCreate(){
		receipt = authReceiptDao.createNewReceipt(userId);
		assertTrue(authReceiptDao.isValidReceipt(userId, receipt));
	}

	@Test
	public void testReplace(){
		receipt = authReceiptDao.createNewReceipt(userId);
		String newReceipt = authReceiptDao.replaceReceipt(userId, receipt);
		assertFalse(receipt.equals(newReceipt));
		assertFalse(authReceiptDao.isValidReceipt(userId, receipt));
		assertTrue(authReceiptDao.isValidReceipt(userId, newReceipt));
	}

	@Test
	public void testCount(){
		assertEquals(0L, authReceiptDao.countReceipts(userId));
		authReceiptDao.createNewReceipt(userId);
		assertEquals(1L, authReceiptDao.countReceipts(userId));
		authReceiptDao.replaceReceipt(userId, receipt);
		assertEquals(1L, authReceiptDao.countReceipts(userId));
	}

	@Test
	public void testDelete(){
		receipt = authReceiptDao.createNewReceipt(userId);
		authReceiptDao.deleteExpiredReceipts(userId, System.currentTimeMillis());
		assertTrue(authReceiptDao.isValidReceipt(userId, receipt));
		authReceiptDao.deleteExpiredReceipts(userId, System.currentTimeMillis()+EXPIRATION_PERIOD);
		assertFalse(authReceiptDao.isValidReceipt(userId, receipt));
	}
}
