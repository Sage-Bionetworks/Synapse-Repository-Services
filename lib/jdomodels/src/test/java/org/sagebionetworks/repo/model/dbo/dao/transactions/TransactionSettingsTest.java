package org.sagebionetworks.repo.model.dbo.dao.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test is designed to ensure we rollback transaction on the types of
 * exceptions that we expect. There is currently only one exception that we do
 * not want to rollback on and that is {@link NotFoundException}. All others
 * exceptions should trigger a rollback.
 * 
 * @author John
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:transaction-test-context.xml" })
public class TransactionSettingsTest {

	@Autowired
	TransactionValidator transactionValidator;

	@Test
	public void testRollbackOnIOException() throws Throwable {
		// call validate with this type of exception
		validateRollbackOnExecption(new IOException());
	}

	@Test
	public void testRollbackOnThroable() throws Throwable {
		// call validate with this type of exception
		validateRollbackOnExecption(new Throwable());
	}

	@Test
	public void testRollbackOnInterruptedException() throws Throwable {
		// call validate with this type of exception
		validateRollbackOnExecption(new InterruptedException());
	}

	@Test
	public void testNoRollbackOnNotFoundException() throws Throwable {
		// call validate with this type of exception
		validateNoRollbackOnExecption(new NotFoundException());
	}

	/**
	 * Validate that an exception of the given type triggers a rollback
	 * 
	 * @param toTest
	 * @throws Throwable
	 */
	public void validateRollbackOnExecption(Throwable toTest) throws Throwable {
		// First set the value to make sure it it works
		String value = "startValue";
		Long id = 1l;
		String result = transactionValidator.setString(id, value, null);
		// This should work
		assertEquals(value, result);
		// Now try to change the value with an exception that should trigger a
		// rollback
		String shouldBeRejected = "shouldBeRejected";
		try {
			transactionValidator.setString(id, shouldBeRejected, toTest);
			fail("Should have thrown an exception");
		} catch (Exception e) {
			// expected;
		}
		// Now make sure the change did not take
		String currentValue = transactionValidator.getString(id);
		assertEquals("A " + toTest.getClass().getName()
				+ " did should have trigger a rollback", value, currentValue);
	}

	/**
	 * Validate that an exception of the given type triggers a rollback
	 * 
	 * @param toTest
	 * @throws Throwable
	 */
	public void validateNoRollbackOnExecption(Throwable toTest)
			throws Throwable {
		// First set the value to make sure it it works
		String value = "startValue";
		Long id = 2l;
		String result = transactionValidator.setString(id, value, null);
		// This should work
		assertEquals(value, result);
		// Now try to change the value with an exception that should trigger a
		// rollback
		String shouldBeAccepted = "shouldBeAccepted";
		try {
			transactionValidator.setString(id, shouldBeAccepted,
					new NotFoundException());
			fail("Should have thrown an exception");
		} catch (Exception e) {
			// expected;
		}
		// Now make sure the change did take
		String currentValue = transactionValidator.getString(id);
		assertEquals("An " + toTest.getClass().getName()
				+ " did triggered a rollback but it should not have",
				shouldBeAccepted, currentValue);
	}

}
