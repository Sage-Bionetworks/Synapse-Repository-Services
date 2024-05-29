package org.sagebionetworks.database.semaphore;

import static org.sagebionetworks.database.semaphore.Sql.COL_TABLE_SEM_LOCK_EXPIRES_ON;
import static org.sagebionetworks.database.semaphore.Sql.COL_TABLE_SEM_LOCK_LOCK_KEY;
import static org.sagebionetworks.database.semaphore.Sql.COL_TABLE_SEM_LOCK_TOKEN;
import static org.sagebionetworks.database.semaphore.Sql.TABLE_SEMAPHORE_LOCK;

import java.sql.ResultSet;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Basic database backed multiple lock semaphore. The semaphore involves two
 * tables, a master table, with a single row per unique lock key, and a lock
 * table that can contain multiple tokens for each lock. All operations on the
 * lock table are gated with a SELECT FOR UPDATE on the master table's key row.
 * This ensure all checks and changes occur serially.
 * </p>
 * 
 * Each operation (acquire, release, refresh) is executed using a MySQL
 * procedure. This ensures the amount of time between acquiring the exclusive
 * lock for a key row and the release of the lock is not gated by the
 * health/speed of calling thread. @See <a
 * href="https://sagebionetworks.jira.com/browse/PLFM-3439">PLFM-3439</a>
 * <p>
 * This class is thread-safe and can be used as a singleton.
 * </p>
 * <p>
 * Note that acquiring, releasing and refreshing a lock needs to run in its own separate transaction.
 * If setup through a spring bean, transaction management can be enabled to do so automatically since
 * each method is annotated as Transactional with REQUIRES_NEW propagation. 
 * </p>
 * 
 * @author John
 * 
 */

public class CountingSemaphoreImpl implements CountingSemaphore {

	public static final int MAX_CONTEXT_CHARS = 256;

	private static final String COUNT_LOCK_ROWS = "SELECT COUNT(*) FROM SEMAPHORE_LOCK";

	private static final String CALL_REFRESH_SEMAPHORE_LOCK = "CALL refreshSemaphoreLock(?, ?)";

	private static final String CALL_RELEASE_SEMAPHORE_LOCK = "CALL releaseSemaphoreLock(?)";

	private static final String CALL_ATTEMPT_TO_ACQUIRE_SEMAPHORE_LOCK = "CALL attemptToAcquireSemaphoreLock(?, ?, ?, ?)";

	private static final String REFRESH_SEMAPHORE_LOCK = "refreshSemaphoreLock";

	private static final String RELEASE_SEMAPHORE_LOCK = "releaseSemaphoreLock";

	private static final String ATTEMPT_TO_ACQUIRE_SEMAPHORE_LOCK = "attemptToAcquireSemaphoreLock";
	
	private static final String BOOTSTRAP_LOCK = "bootstrapLockKeyRows";
	
	private static final String GARBAGE_COLLECTION = "runGarbageCollection";

	private static final Logger log = LogManager
			.getLogger(CountingSemaphoreImpl.class);

	private static final String SQL_CLEAR_ALL_LOCKS = "UPDATE "+ TABLE_SEMAPHORE_LOCK+" SET TOKEN = NULL, EXPIRES_ON = CURRENT_TIMESTAMP - INTERVAL 10 SECOND WHERE LOCK_KEY IS NOT NULL";

	private static final String SQL_EXISTS_UNEXPIRED_LOCK =
			"SELECT CONTEXT FROM " + TABLE_SEMAPHORE_LOCK + " WHERE " + COL_TABLE_SEM_LOCK_LOCK_KEY + " = ?" +
			" AND " + COL_TABLE_SEM_LOCK_TOKEN + " IS NOT NULL " +
			" AND " + COL_TABLE_SEM_LOCK_EXPIRES_ON + " >= CURRENT_TIMESTAMP" +
			" LIMIT 1";
	

	private static final String SEMAPHORE_LOCK_DDL_SQL = "schema/SemaphoreLock.ddl.sql";
	private static final String PROCEDURE_DDL_SQL_TEMPLATE = "schema/%s.ddl.sql";
	private static final String PROCEDURE_EXITS_TEMPLATE = "PROCEDURE %s already exists";
	
	private JdbcTemplate jdbcTemplate;

	/**
	 * Create a new CountingkSemaphore. This implementation depends on two
	 * database tables (SEMAPHORE_MASTER & SEMAPHORE_LOCK) that will be created
	 * if they do not exist.
	 * 
	 * @param dataSourcePool
	 *            Must be a connection to a MySql Database, ideally a database
	 *            connection pool.
	 * @param transactionManager
	 *            The singleton transaction manager.
	 * 
	 */
	public CountingSemaphoreImpl(DataSource dataSourcePool) {
		if (dataSourcePool == null) {
			throw new IllegalArgumentException("DataSource cannot be null");
		}
		jdbcTemplate = new JdbcTemplate(dataSourcePool);
		// Create the tables
		this.jdbcTemplate.update(Utils
				.loadStringFromClassPath(SEMAPHORE_LOCK_DDL_SQL));
		createProcedureIfDoesNotExist(BOOTSTRAP_LOCK);
		createProcedureIfDoesNotExist(ATTEMPT_TO_ACQUIRE_SEMAPHORE_LOCK);
		createProcedureIfDoesNotExist(RELEASE_SEMAPHORE_LOCK);
		createProcedureIfDoesNotExist(REFRESH_SEMAPHORE_LOCK);
		createProcedureIfDoesNotExist(GARBAGE_COLLECTION);
	}

	/**
	 * Load the procedure ddl file and create it if it does not exist.
	 * 
	 * @param name
	 */
	private void createProcedureIfDoesNotExist(String name) {
		try {
			this.jdbcTemplate.update(Utils.loadStringFromClassPath(String.format(PROCEDURE_DDL_SQL_TEMPLATE, name)));
		} catch (DataAccessException e) {
			String message = String.format(PROCEDURE_EXITS_TEMPLATE, name);
			if (e.getMessage().contains(message)) {
				log.info(message);
			} else {
				throw e;
			}
		}
	}
	
	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public Optional<String> attemptToAcquireLock(final String key, final long timeoutSec,
			final int maxLockCount, final String inputContext) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (timeoutSec < 1) {
			throw new IllegalArgumentException("TimeoutSec cannot be less then one.");
		}
		if (maxLockCount < 1) {
			throw new IllegalArgumentException("MaxLockCount cannot be less then one.");
		}
		if (inputContext == null || inputContext.isBlank()) {
			throw new IllegalArgumentException("Context cannot be null or empty");
		}
		if (inputContext.length() > MAX_CONTEXT_CHARS) {
			throw new IllegalArgumentException("Context length cannot be more than: "+MAX_CONTEXT_CHARS);
		}
		try {
			return jdbcTemplate.queryForObject(CALL_ATTEMPT_TO_ACQUIRE_SEMAPHORE_LOCK, (ResultSet rs, int rowNum) -> {
				return Optional.ofNullable(rs.getString("TOKEN"));
			}, key, timeoutSec, maxLockCount, inputContext);
		} catch (TransientDataAccessException e) {
			log.warn("Failed to acquire lock on key {}: {}", key, e.getMessage(), e);
			return Optional.empty();
		}

	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public void releaseLock(final String key, final String token) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null.");
		}
		int result = jdbcTemplate.queryForObject(CALL_RELEASE_SEMAPHORE_LOCK,
				Integer.class, token);
		Utils.validateResults(key, token, result);

	}

	@Override
	public void releaseAllLocks() {
		jdbcTemplate.update(SQL_CLEAR_ALL_LOCKS);
	}
	
	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public void refreshLockTimeout(final String key, final String token,
			final long timeoutSec) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null.");
		}
		if (timeoutSec < 1) {
			throw new IllegalArgumentException(
					"TimeoutSec cannot be less then one.");
		}
		int result = jdbcTemplate.queryForObject(CALL_REFRESH_SEMAPHORE_LOCK,
				Integer.class, token, timeoutSec);
		Utils.validateResults(key, token, result);
	}

	@Override
	public Optional<String> getFirstUnexpiredLockContext(final String key) {
		try {
			return Optional.of(jdbcTemplate.queryForObject(SQL_EXISTS_UNEXPIRED_LOCK, String.class, key));
		}catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
	public void runGarbageCollection() {
		jdbcTemplate.update("CALL runGarbageCollection();");
	}

	@Override
	public long getLockRowCount() {
		return jdbcTemplate.queryForObject(COUNT_LOCK_ROWS, Long.class) ;
	}

}
