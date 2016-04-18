package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

	@Test
	public void testReceiptInvalid() {
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
}
