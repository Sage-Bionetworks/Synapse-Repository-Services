package org.sagebionetworks.repo.model.dbo.dao.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ThreadStepper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * This test is designed to ensure we rollback transaction on the types of exceptions that we expect. There is currently
 * only one exception that we do not want to rollback on and that is {@link RecordNotFoundException}. All others
 * exceptions should trigger a rollback.
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
	public void validateRequiredTransaction() {
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
						transactionValidator.setStringNoTransaction(id, "written");
						stepper.stepDone("written");
						stepper.waitForStepDone("verify still no write yet");
						return null;
					}
				});
				stepper.stepDone("committed");
				stepper.waitForStepDone("verify commit");
				return null;
			}
		});
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				stepper.waitForStepDone("no write yet");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify no write yet");
				stepper.waitForStepDone("written");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify still no write yet");
				stepper.waitForStepDone("committed");
				assertEquals("written", transactionValidator.getString(id));
				stepper.stepDone("verify commit");
				return null;
			}
		});
		stepper.run();
	}

	@Test
	public void validateRequiredTransactionRollback() {
		final Long id = 2l;
		final ThreadStepper stepper = new ThreadStepper(10);
		transactionValidator.setStringNoTransaction(id, "none");
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					transactionValidator.required(new Callable<String>() {
						@Override
						public String call() throws Exception {
							stepper.stepDone("no write yet");
							stepper.waitForStepDone("verify no write yet");
							transactionValidator.setStringNoTransaction(id, "written");
							stepper.stepDone("written");
							stepper.waitForStepDone("verify still no write yet");
							throw new Exception("test");
						}
					});
					fail("shouldn't get here");
				} catch (Exception e) {
					assertEquals("test", e.getMessage());
					assertEquals(Exception.class, e.getClass());
					stepper.stepDone("rollback");
					stepper.waitForStepDone("verify rollback");
				}
				return null;
			}
		});
		stepper.add(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				stepper.waitForStepDone("no write yet");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify no write yet");
				stepper.waitForStepDone("written");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify still no write yet");
				stepper.waitForStepDone("rollback");
				assertEquals("none", transactionValidator.getString(id));
				stepper.stepDone("verify rollback");
				return null;
			}
		});
		stepper.run();
	}

	@Test
	public void validateMandatoryTransaction() throws Exception {
		assertEquals("done", transactionValidator.required(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return transactionValidator.mandatory(new Callable<String>() {
					@Override
					public String call() throws Exception {
						return "done";
					}
				});
			}
		}));

		try {
			transactionValidator.mandatory(new Callable<String>() {
				@Override
				public String call() throws Exception {
					fail("Should not get here");
					return "done";
				}
			});
			fail("Should have failed");
		} catch (IllegalTransactionStateException e) {
		}
	}
}
