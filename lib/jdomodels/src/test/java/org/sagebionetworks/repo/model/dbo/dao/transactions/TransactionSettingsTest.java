package org.sagebionetworks.repo.model.dbo.dao.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.util.ThreadStepper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test is designed to ensure we rollback transaction on the types of exceptions that we expect. There is currently
 * only one exception that we do not want to rollback on and that is {@link RecordNotFoundException}. All others
 * exceptions should trigger a rollback.
 * 
 * @author John
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
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
	public void testRollbackOnError() throws Throwable {
		// call validate with this type of exception
		validateRollbackOnExecption(new Error());
	}

	@Test
	public void testRollbackOnInterruptedException() throws Throwable {
		// call validate with this type of exception
		validateRollbackOnExecption(new InterruptedException());
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
		final String shouldBeRejected = "shouldBeRejected";
		try {
			transactionValidator.setString(id, shouldBeRejected, toTest);
			fail("Should have thrown an exception");
		} catch (Throwable e) {
			assertEquals(toTest, e);

		}
		// Now make sure the change did not take
		String currentValue = transactionValidator.getString(id);
		assertEquals("A " + toTest.getClass().getName()
				+ " did should have trigger a rollback", value, currentValue);

		try {
			transactionValidator.setStringLevel2(id, shouldBeRejected, toTest);
			fail("Should have thrown an exception");
		} catch (Throwable e) {
			assertEquals(toTest, e);

		}
		// Now make sure the change did not take
		currentValue = transactionValidator.getString(id);
		assertEquals("A " + toTest.getClass().getName() + " did should have trigger a rollback", value, currentValue);
	}

	@Test
	public void validateNewTransaction() {
		final Long id = 2l;
		final ThreadStepper stepper = new ThreadStepper(10);
		transactionValidator.setStringNoTransaction(id, "none");
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				transactionValidator.required(new Callable<String>() {
					@Override
					public String call() throws Exception {
						stepper.stepDone("no write yet");
						stepper.waitForStepDone("verify no write yet");
						transactionValidator.requiresNew(new Callable<String>() {
							@Override
							public String call() throws Exception {
								transactionValidator.setStringNoTransaction(id, "written");
								return null;
							}
						});
						stepper.stepDone("write");
						stepper.waitForStepDone("verify write");
						return null;
					}
				});
				return null;
			}
		});
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				stepper.waitForStepDone("no write yet");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify no write yet");
				stepper.waitForStepDone("write");
				assertEquals("written", transactionValidator.getString(id));
				stepper.stepDone("verify write");
				return null;
			}
		});
		stepper.run();
	}





	
	@Test
	public void validateWriteReadCommittedTransaction() {
		final Long id = 3l;
		final ThreadStepper stepper = new ThreadStepper(10);
		transactionValidator.setStringNoTransaction(id, "startValue");
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				// start a transaction with read-committed.
				transactionValidator.writeReadCommitted(new Callable<String>() {
					@Override
					public String call() throws Exception {
						stepper.stepDone("first started");
						stepper.waitForStepDone("second started");
						// update the row
						transactionValidator.setStringNoTransaction(id, "updatedValue");
						stepper.stepDone("first insert");
						stepper.waitForStepDone("second check");
						return null;
					}
				});
				// the first transaction is now committed
				stepper.stepDone("first commit");
				return null;
			}
		});
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				transactionValidator.writeReadCommitted(new Callable<String>() {
					@Override
					public String call() throws Exception {
						stepper.waitForStepDone("first started");
						assertEquals("Before either starts the row should be null", "startValue", transactionValidator.getString(id));
						stepper.stepDone("second started");
						stepper.waitForStepDone("first insert");
						assertEquals("Since the other transaction has not committed should not be able to see the insert.","startValue", transactionValidator.getString(id));
						stepper.stepDone("second check");
						stepper.waitForStepDone("first commit");
						assertEquals("Cannot see the value from a commited transaction.","updatedValue", transactionValidator.getString(id));
						return null;
					}
				});
				return null;
			}
		});
		stepper.run();
	}
	
	@Test
	public void validateNewWriteTransaction() throws Exception {
		final Long idOne = 4l;
		final Long idTwo = 5l;
		try{
			// Start a transaction
			transactionValidator.writeReadCommitted(new Callable<String>() {
				
				@Override
				public String call() throws Exception {
					// Change the value within this transaction.
					transactionValidator.setStringNoTransaction(idOne, "shouldRollback");
					// Change the value in a new transaction
					transactionValidator.NewWriteTransaction(new Callable<String>() {
						
						@Override
						public String call() throws Exception {
							// This should not rollback.
							transactionValidator.setStringNoTransaction(idTwo, "shouldNotRollback");
							return null;
						}
					});
					// trigger the rollback of the outer transaction
					throw new IllegalArgumentException("Thrown to trigger a rollback");
				}
			});
			fail("Should have thrown an exception");
		}catch(IllegalArgumentException e){
			// expected
		}
		// id two should be committed.
		String result = transactionValidator.getString(idTwo);
		assertEquals("shouldNotRollback", result);
	}
}
