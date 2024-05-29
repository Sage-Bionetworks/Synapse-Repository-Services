package org.sagebionetworks.database.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.spb.xml" })
public class CountingSemaphoreImplTest {

	private static final Logger log = LogManager.getLogger(CountingSemaphoreImplTest.class);

	@Autowired
	private CountingSemaphore semaphore;

	@Autowired
	private DataSourceTransactionManager txManager;

	private String key;
	private String context;

	@BeforeEach
	public void before() {
		semaphore.releaseAllLocks();
		key = "sampleKey";
		context = "sample context";
	}

	@Test
	public void testAttemptToAcquireLockWithNullKey() {
		key = null;
		int maxLockCount = 2;
		long timeoutSec = 60;
		context = "some context";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		}).getMessage();
		assertEquals("Key cannot be null", message);
	}

	@Test
	public void testAttemptToAcquireLockWithNullContext() {
		key = "aKey";
		int maxLockCount = 2;
		long timeoutSec = 60;
		context = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		}).getMessage();
		assertEquals("Context cannot be null or empty", message);
	}

	@Test
	public void testAttemptToAcquireLockWithEmptyContext() {
		key = "aKey";
		int maxLockCount = 2;
		long timeoutSec = 60;
		context = " \t";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		}).getMessage();
		assertEquals("Context cannot be null or empty", message);
	}

	@Test
	public void testAttemptToAcquireLockWithContextAtMaxLength() {
		key = "aKey";
		int maxLockCount = 2;
		long timeoutSec = 60;
		context = "a".repeat(CountingSemaphoreImpl.MAX_CONTEXT_CHARS);
		Optional<String> token = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token.isPresent());
		Optional<String> contextOp = semaphore.getFirstUnexpiredLockContext(key);
		assertEquals(Optional.of(context), contextOp);
	}

	@Test
	public void testAttemptToAcquireLockWithContextOverLimit() {
		key = "aKey";
		int maxLockCount = 2;
		long timeoutSec = 60;
		context = "a".repeat(CountingSemaphoreImpl.MAX_CONTEXT_CHARS + 1);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		}).getMessage();
		assertEquals("Context length cannot be more than: " + CountingSemaphoreImpl.MAX_CONTEXT_CHARS, message);
	}

	@Test
	public void testAcquireRelease() {
		int maxLockCount = 2;
		long timeoutSec = 60;
		// get one lock
		long start = System.currentTimeMillis();
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		log.info("AcquiredLock in " + (System.currentTimeMillis() - start) + " MS");
		// get another
		Optional<String> token2 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token2.isPresent());
		// Try for a third should not acquire a lock
		Optional<String> token3 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertFalse(token3.isPresent());
		// release
		semaphore.releaseLock(key, token2.get());
		// we should now be able to get a new lock
		token3 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertNotNull(token3);
	}

	@Test
	public void testLockExpired() throws InterruptedException {
		int maxLockCount = 1;
		long timeoutSec = 1;
		// get one lock
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		// Should not be able to get a lock
		Optional<String> token2 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertFalse(token2.isPresent());
		// Wait for the lock first lock to expire
		Thread.sleep(timeoutSec * 1000 * 2);
		// We should now be able to get the lock as the first is expired.
		token2 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token2.isPresent());
	}

	@Test
	public void testReleaseExpiredLock() throws InterruptedException {
		int maxLockCount = 1;
		long timeoutSec = 1;
		// get one lock
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		// Wait until the lock expires
		Thread.sleep(timeoutSec * 1000 * 2);
		// another should be able to get the lock
		Optional<String> token2 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token2.isPresent());
		assertThrows(LockReleaseFailedException.class, () -> {
			// this should fail as the lock has already expired.
			semaphore.releaseLock(key, token1.get());
		});
	}

	@Test
	public void testRefreshLockTimeout() throws InterruptedException {
		int maxLockCount = 1;
		long timeoutSec = 2;
		// get one lock
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		// We should be able to refresh the lock.
		for (int i = 0; i < timeoutSec + 1; i++) {
			semaphore.refreshLockTimeout(key, token1.get(), timeoutSec);
			Thread.sleep(1000);
		}
		// The lock should still be held even though we have now exceeded to original
		// timeout.
		semaphore.releaseLock(key, token1.get());
	}

	@Test
	public void testRefreshExpiredLock() throws InterruptedException {
		int maxLockCount = 1;
		long timeoutSec = 1;
		// get one lock
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		// Wait until the lock expires
		Thread.sleep(timeoutSec * 1000 * 2);
		// another should be able to get the lock
		Optional<String> token2 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token2.isPresent());
		assertThrows(LockReleaseFailedException.class, () -> {
			// this should fail as the lock has already expired.
			semaphore.refreshLockTimeout(key, token1.get(), timeoutSec);
		});
	}

	@Test
	public void testReleaseLockAfterReleaseAllLocks() {
		int maxLockCount = 1;
		long timeoutSec = 1;
		// get one lock
		Optional<String> token1 = semaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, context);
		assertTrue(token1.isPresent());
		// Force the release of all locks
		semaphore.releaseAllLocks();
		assertThrows(LockReleaseFailedException.class, () -> {
			// Now try to release the lock
			semaphore.releaseLock(key, token1.get());
		});
	}

	/**
	 * Test concurrent threads can acquire and release locks
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConcurrent() throws Exception {
		int maxThreads = 25;
		long lockTimeoutSec = 20;
		int maxLockCount = maxThreads - 1;
		ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
		List<Callable<Boolean>> runners = new LinkedList<Callable<Boolean>>();
		for (int i = 0; i < maxThreads; i++) {
			TestRunner runner = new TestRunner(semaphore, key, lockTimeoutSec, maxLockCount, context);
			runners.add(runner);
		}
		// run all runners
		List<Future<Boolean>> futures = executorService.invokeAll(runners);
		int locksAcquired = countLocksAcquired(futures);
		assertEquals(maxLockCount, locksAcquired, "24 of 25 threads should have been issued a lock");
	}

	/**
	 * If two process attempt to get two separate locks at the same time the the
	 * 'NOWAIT' condition should not trigger, and each process should receive a
	 * lock.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConcurrentDifferentKeys() throws Exception {
		int maxThreads = 25;
		long lockTimeoutSec = 20;
		int maxLocksPerThread = 1;
		// create a different key for each thread.
		List<String> keys = createUniqueKeys(maxThreads, maxLocksPerThread);
		ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
		List<Callable<Boolean>> runners = new LinkedList<Callable<Boolean>>();
		for (String key : keys) {
			TestRunner runner = new TestRunner(semaphore, key, lockTimeoutSec, maxLocksPerThread, context);
			runners.add(runner);
		}
		// run all runners
		List<Future<Boolean>> futures = executorService.invokeAll(runners);
		int locksAcquired = countLocksAcquired(futures);
		assertTrue(locksAcquired >= maxThreads - 3, "Most threads should have received a lock");
	}

	private int countLocksAcquired(List<Future<Boolean>> futures)
			throws InterruptedException, java.util.concurrent.ExecutionException {
		int locksAcquired = 0;
		for (Future<Boolean> future : futures) {
			if (future.get()) {
				locksAcquired++;
			}
		}
		return locksAcquired;
	}

	private void holdLocksOfSameKeyWithTimeouts(String lockKey, List<Long> lockTimeouts, String context)
			throws InterruptedException, java.util.concurrent.ExecutionException {
		int locksAcquired = 0;
		for (long timeoutSec : lockTimeouts) {
			Optional<String> token = semaphore.attemptToAcquireLock(lockKey, timeoutSec, lockTimeouts.size(), context);
			if (token.isPresent()) {
				locksAcquired++;
			}
		}

		assertEquals(lockTimeouts.size(), locksAcquired);
	}

	@Test
	public void testExistsUnexpiredLock_notExist() throws Exception {
		// set up unexpired locks held by other threads with a different key;
		String unrelatedLockKey = "unrelatedLock";
		List<Long> lockTimeouts = Collections.nCopies(5, 50L); // 5 locks w/ expiration of 50 seconds each
		holdLocksOfSameKeyWithTimeouts(unrelatedLockKey, lockTimeouts, context);
		// method under test
		assertEquals(Optional.empty(), semaphore.getFirstUnexpiredLockContext("otherKey"));
	}

	@Test
	public void testExistsUnexpiredLock_existButAllExpired() throws ExecutionException, InterruptedException {
		// set up locks that will expire
		String lockKey = "sameKey";
		List<Long> lockTimeouts = Collections.nCopies(5, 1L); // 5 locks w/ expiration of 1 second each
		holdLocksOfSameKeyWithTimeouts(lockKey, lockTimeouts, context);
		Thread.sleep(2000);

		// method under test
		assertEquals(Optional.empty(), semaphore.getFirstUnexpiredLockContext(lockKey));
	}

	@Test
	public void testExistsUnexpiredLock_existAndSomeUnexpired() throws ExecutionException, InterruptedException {
		// set up locks that will expire
		String lockKey = "sameKey";
		List<Long> lockTimeouts = Arrays.asList(1L, 1L, 600L, 1L, 1L);
		holdLocksOfSameKeyWithTimeouts(lockKey, lockTimeouts, context);
		Thread.sleep(1000);

		// method under test
		assertEquals(Optional.of(context), semaphore.getFirstUnexpiredLockContext(lockKey));
	}

	@Test
	public void testGarbageCollection() throws InterruptedException {
		// Start clean
		semaphore.runGarbageCollection();
		assertEquals(0, semaphore.getLockRowCount());
		
		long lockTimeoutSec = 2;
		int maxLockCount = 3;
		semaphore.attemptToAcquireLock("keyOne", lockTimeoutSec, maxLockCount, context);
		assertEquals(3, semaphore.getLockRowCount());
		// set all three rows to be expired and therefore eligible for garbage collection.
		semaphore.releaseAllLocks();
		assertEquals(3, semaphore.getLockRowCount());
		// add three new rows
		semaphore.attemptToAcquireLock("keyTwo", lockTimeoutSec, maxLockCount, context);
		assertEquals(6, semaphore.getLockRowCount());
		String keyTwoTokenTwo = semaphore.attemptToAcquireLock("keyTwo", lockTimeoutSec, maxLockCount, context).get();
		// releasing a lock clears its token but it should not expire for at least 5 minutes.
		semaphore.releaseLock("keyTwo", keyTwoTokenTwo);
		assertEquals(6, semaphore.getLockRowCount());
		
		// call under test
		semaphore.runGarbageCollection();
		// Garbage collection should only remove the first three throw since their tokens are null and they are expired (due to releaseAllLocks()).
		assertEquals(3, semaphore.getLockRowCount());
		semaphore.releaseAllLocks();
		
		// call under test
		semaphore.runGarbageCollection();
		assertEquals(0, semaphore.getLockRowCount());
	}

	@Test
	public void testAttemptToAcquireLockInNewTransaction() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());

		DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();

		txDef.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionTemplate txTemplate = new TransactionTemplate(txManager, txDef);

		assertThrows(RuntimeException.class, () -> {
			txTemplate.executeWithoutResult((txStatus) -> {
				jdbcTemplate.update("INSERT INTO SEMAPHORE_LOCK VALUES(-1, 'someKey', 0, NULL, NOW(), NULL)");

				// Call under test
				semaphore.attemptToAcquireLock("key", 5, 1, context);

				throw new RuntimeException("Something went wrong");
			});
		});

		assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'someKey'",
				Long.class));
		assertEquals(1L,
				jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'key'", Long.class));
	}

	@Test
	public void testReleaseLockInNewTransaction() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());

		String token = semaphore.attemptToAcquireLock("key", 5, 1, context).get();

		DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();

		txDef.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionTemplate txTemplate = new TransactionTemplate(txManager, txDef);

		assertThrows(RuntimeException.class, () -> {
			txTemplate.executeWithoutResult((txStatus) -> {
				jdbcTemplate.update("INSERT INTO SEMAPHORE_LOCK VALUES(-1, 'someKey', 0, NULL, NOW(), NULL)");

				// Call under test
				semaphore.releaseLock("key", token);

				throw new RuntimeException("Something went wrong");
			});
		});

		assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'someKey'",
				Long.class));
		assertEquals(null,
				jdbcTemplate.queryForObject("SELECT TOKEN FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'key'", String.class));
	}

	@Test
	public void testRefreshLockInNewTransaction() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());

		String token = semaphore.attemptToAcquireLock("key", 5, 1, context).get();

		Timestamp expireOn = jdbcTemplate.queryForObject("SELECT EXPIRES_ON FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'key'",
				Timestamp.class);

		DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();

		txDef.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionTemplate txTemplate = new TransactionTemplate(txManager, txDef);

		assertThrows(RuntimeException.class, () -> {
			txTemplate.executeWithoutResult((txStatus) -> {
				jdbcTemplate.update("INSERT INTO SEMAPHORE_LOCK VALUES(-1, 'someKey', 0, NULL, NOW(), NULL)");

				// Call under test
				semaphore.refreshLockTimeout("key", token, 10);

				throw new RuntimeException("Something went wrong");
			});
		});

		assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'someKey'",
				Long.class));
		assertNotEquals(expireOn, jdbcTemplate
				.queryForObject("SELECT EXPIRES_ON FROM SEMAPHORE_LOCK WHERE LOCK_KEY = 'key'", Timestamp.class));
	}

	@Test
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	public void testAttemptToAcquireSemaphoreLockWithBootstraping() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(txManager.getDataSource());

		// clear all of the rows rows in the semaphore table to guarantee that bootstraping will insert new rows.
		semaphore.releaseAllLocks();
		semaphore.runGarbageCollection();

		DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();

		txDef.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
		txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		TransactionTemplate txTemplate = new TransactionTemplate(txManager, txDef);

		String key = "aKey";
		int timeoutSec = 30;
		int maxLockCount = 5;
		String inputContext = "some context";

		txTemplate.executeWithoutResult((txStatus) -> {

			Optional<String> firstToken = directAttemptToAcquireSemaphoreLock(jdbcTemplate, key, timeoutSec,
					maxLockCount, inputContext);
			assertTrue(firstToken.isPresent());

			/*
			 * At this point the first transaction has not been commited.  We must be able to
			 * get a second token in a new transaction even though the first transaction has
			 * not been commited yet.
			 */
			txTemplate.executeWithoutResult((tx) -> {
				Optional<String> secondToken = directAttemptToAcquireSemaphoreLock(jdbcTemplate, key, timeoutSec,
						maxLockCount, inputContext);
				assertTrue(secondToken.isPresent());
			});

		});
	}

	/**
	 * A direct call to attemptToAcquireSemaphoreLock without transactions
	 * annotations.
	 * 
	 * @param template
	 * @param key
	 * @param timeoutSec
	 * @param maxLockCount
	 * @param inputContext
	 * @return
	 */
	Optional<String> directAttemptToAcquireSemaphoreLock(JdbcTemplate template, String key, int timeoutSec,
			int maxLockCount, String inputContext) {
		return template.queryForObject("CALL attemptToAcquireSemaphoreLock(?, ?, ?, ?)", (ResultSet rs, int rowNum) -> {
			return Optional.ofNullable(rs.getString("TOKEN"));
		}, key, timeoutSec, maxLockCount, inputContext);
	}

	/**
	 * Create n unique keys and ensure each key already exists in the database.
	 * 
	 * @param count
	 * @return
	 */
	public List<String> createUniqueKeys(int count, int maxKeys) {
		List<String> keys = new LinkedList<String>();
		for (int i = 0; i < count; i++) {
			String key = "i-" + i;
			Optional<String> token = semaphore.attemptToAcquireLock(key, 1000, maxKeys, context);
			semaphore.releaseLock(key, token.get());
			keys.add(key);
		}
		return keys;
	}

	private class TestRunner implements Callable<Boolean> {
		CountingSemaphore semaphore;
		String key;
		long lockTimeoutSec;
		int maxLockCount;
		long sleepTimeMs;
		String context;

		public TestRunner(CountingSemaphore semaphore, String key, long lockTimeoutSec, int maxLockCount,
				String context) {
			super();
			this.semaphore = semaphore;
			this.key = key;
			this.lockTimeoutSec = lockTimeoutSec;
			this.maxLockCount = maxLockCount;
			this.sleepTimeMs = 1000L;
			this.context = context;
		}

		public Boolean call() throws Exception {
			long start = System.currentTimeMillis();
			Optional<String> result = semaphore.attemptToAcquireLock(key, lockTimeoutSec, maxLockCount, context);

			log.info("AttemptToAcquiredLock in " + (System.currentTimeMillis() - start) + " MS with token: "
					+ result.orElseGet(() -> null));
			if (result.isPresent()) {
				try {
					Thread.sleep(sleepTimeMs);
					// the lock was acquired and held
					return true;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} finally {
					semaphore.releaseLock(key, result.get());
				}
			} else {
				// lock was not acquired
				return false;
			}
		}
	}

}
