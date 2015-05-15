package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SEM_LOCK_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SEM_LOCK_LOCK_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SEM_LOCK_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_SEM_MAST_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEMAPHORE_LOCK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEMAPHORE_MASTER;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.exception.LockReleaseFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Basic database backed multiple lock semaphore. The semaphore involves two
 * tables, a master table, with a single row per unique lock key, and a lock
 * table that can contain multiple tokens for each lock. All operations on the
 * lock table are gated with a SELECT FOR UPDATE on the master table's key row.
 * This ensure all checks and changes occur serially.
 * 
 * @author John
 * 
 */
public class MultipleLockSemaphoreImpl implements MultipleLockSemaphore {

	private static final String SQL_UPDATE_LOCK_EXPIRES = "UPDATE "
			+ TABLE_SEMAPHORE_LOCK + " SET " + COL_TABLE_SEM_LOCK_EXPIRES_ON
			+ " = (CURRENT_TIMESTAMP + INTERVAL ? SECOND) WHERE "
			+ COL_TABLE_SEM_LOCK_LOCK_KEY + " = ? AND "
			+ COL_TABLE_SEM_LOCK_TOKEN + " = ?";
	
	private static final String SQL_DELETE_ALL_MASTER = "DELETE FROM "
			+ TABLE_SEMAPHORE_MASTER + " WHERE " + COL_TABLE_SEM_MAST_KEY
			+ " IS NOT NULL";
	
	private static final String SQL_DELETE_LOCK_WITH_TOKEN = "DELETE FROM "
			+ TABLE_SEMAPHORE_LOCK + " WHERE " + COL_TABLE_SEM_LOCK_TOKEN
			+ " = ?";
	
	private static final String SQL_INSERT_NEW_LOCK = "INSERT INTO "
			+ TABLE_SEMAPHORE_LOCK + "(" + COL_TABLE_SEM_LOCK_LOCK_KEY + ", "
			+ COL_TABLE_SEM_LOCK_TOKEN + ", " + COL_TABLE_SEM_LOCK_EXPIRES_ON
			+ ") VALUES (?, ?, (CURRENT_TIMESTAMP + INTERVAL ? SECOND))";
	
	private static final String SQL_COUNT_OUTSTANDING_LOCKS = "SELECT COUNT(*) FROM "
			+ TABLE_SEMAPHORE_LOCK
			+ " WHERE "
			+ COL_TABLE_SEM_LOCK_LOCK_KEY
			+ " = ?";
	
	private static final String SQL_DELETE_EXPIRED_LOCKS = "DELETE FROM "
			+ TABLE_SEMAPHORE_LOCK + " WHERE " + COL_TABLE_SEM_LOCK_LOCK_KEY
			+ " = ? AND " + COL_TABLE_SEM_LOCK_EXPIRES_ON
			+ " < CURRENT_TIMESTAMP";
	
	private static final String SQL_SELECT_MASTER_KEY_FOR_UPDATE = "SELECT "
			+ COL_TABLE_SEM_MAST_KEY + " FROM " + TABLE_SEMAPHORE_MASTER
			+ " WHERE " + COL_TABLE_SEM_MAST_KEY + " = ? FOR UPDATE";
	
	private static final String SQL_INSERT_IGNORE_MASTER = "INSERT IGNORE INTO "
			+ TABLE_SEMAPHORE_MASTER
			+ " ("
			+ COL_TABLE_SEM_MAST_KEY
			+ ") VALUES (?)";

	private static final String SEMAPHORE_LOCK_DDL_SQL = "schema/SemaphoreLock.ddl.sql";
	private static final String SEMAPHORE_MASTER_DDL_SQL = "schema/SemaphoreMaster.ddl.sql";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;
	
	TransactionTemplate requiresNewTransactionTempalte;
	
	@PostConstruct
	public void init() {
		DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
		transactionDef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		transactionDef.setReadOnly(false);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDef.setName("MultipleLockSemaphoreImpl");
		// This will manage transactions for calls that need it.
		requiresNewTransactionTempalte = new TransactionTemplate(this.transactionManager, transactionDef);
		
		// Create the tables
		this.jdbcTemplate.update(loadStringFromClassPath(SEMAPHORE_MASTER_DDL_SQL));
		this.jdbcTemplate.update(loadStringFromClassPath(SEMAPHORE_LOCK_DDL_SQL));
	}
	
	/**
	 * Simple utility to load a class path file as a string.
	 * @param fileName
	 * @return
	 */
	public static String loadStringFromClassPath(String fileName){
		InputStream in = MultipleLockSemaphoreImpl.class.getClassLoader().getResourceAsStream(fileName);
		if(in == null){
			throw new IllegalArgumentException("Cannot find: "+fileName+" on the classpath");
		}
		try {
			return IOUtils.toString(in, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.warehouse.workers.semaphore.MultipleLockSemaphore
	 * #attemptToAquireLock(java.lang.String, long, int)
	 */
	public String attemptToAcquireLock(final String key, final long timeoutSec,
			final int maxLockCount) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (timeoutSec < 1) {
			throw new IllegalArgumentException(
					"TimeoutSec cannot be less then one.");
		}
		if (maxLockCount < 1) {
			throw new IllegalArgumentException(
					"MaxLockCount cannot be less then one.");
		}
		// This need to occur in a transaction.
		return requiresNewTransactionTempalte
				.execute(new TransactionCallback<String>() {

					public String doInTransaction(TransactionStatus status) {
						// step one, ensure we have a master lock
						jdbcTemplate.update(SQL_INSERT_IGNORE_MASTER, key);
						// Now lock the master row. This ensure all operations
						// on this
						// key occur serially.
						jdbcTemplate.queryForObject(
								SQL_SELECT_MASTER_KEY_FOR_UPDATE, String.class,
								key);
						// delete expired locks
						jdbcTemplate.update(SQL_DELETE_EXPIRED_LOCKS, key);
						// Count the remaining locks
						long count = jdbcTemplate.queryForObject(
								SQL_COUNT_OUTSTANDING_LOCKS, Long.class, key);
						if (count < maxLockCount) {
							// issue a lock
							String token = UUID.randomUUID().toString();
							jdbcTemplate.update(SQL_INSERT_NEW_LOCK, key, token,
									timeoutSec);
							return token;
						}
						// No token for you!
						return null;
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.warehouse.workers.semaphore.MultipleLockSemaphore
	 * #releaseLock(java.lang.String, java.lang.String)
	 */
	public void releaseLock(final String key, final String token) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (token == null) {
			throw new IllegalArgumentException("Token cannot be null.");
		}
		// This need to occur in a transaction.
		requiresNewTransactionTempalte.execute(new TransactionCallback<Void>() {

			public Void doInTransaction(TransactionStatus status) {
				// Now lock the master row. This ensure all operations on this
				// key occur serially.
				jdbcTemplate.queryForObject(SQL_SELECT_MASTER_KEY_FOR_UPDATE,
						String.class, key);
				// delete expired locks
				int changes = jdbcTemplate
						.update(SQL_DELETE_LOCK_WITH_TOKEN, token);
				if (changes < 1) {
					throw new LockReleaseFailedException("Key: " + key + " token: "
							+ token + " has expired.");
				}
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.warehouse.workers.semaphore.MultipleLockSemaphore
	 * #releaseAllLocks()
	 */
	public void releaseAllLocks() {
		jdbcTemplate.update(SQL_DELETE_ALL_MASTER);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.warehouse.workers.semaphore.MultipleLockSemaphore
	 * #refreshLockTimeout(java.lang.String, java.lang.String, long)
	 */
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
		// This need to occur in a transaction.
		requiresNewTransactionTempalte.execute(new TransactionCallback<Void>() {

			public Void doInTransaction(TransactionStatus status) {
				// Now lock the master row. This ensure all operations on this
				// key occur serially.
				jdbcTemplate.queryForObject(SQL_SELECT_MASTER_KEY_FOR_UPDATE,
						String.class, key);
				// Add more time to the lock.
				int changes = jdbcTemplate.update(SQL_UPDATE_LOCK_EXPIRES,
						timeoutSec, key, token);
				if (changes < 1) {
					throw new LockReleaseFailedException("Key: " + key + " token: "
							+ token + " has expired.");
				}
				return null;
			}
		});
	}

}
