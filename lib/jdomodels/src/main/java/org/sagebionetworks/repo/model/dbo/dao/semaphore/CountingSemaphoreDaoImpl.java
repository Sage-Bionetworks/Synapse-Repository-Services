package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COUNTING_SEMAPHORE_EXPIRES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COUNTING_SEMAPHORE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COUNTING_SEMAPHORE_LOCK_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_LOCK_MASTER_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COUNTING_SEMAPHORE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_LOCK_MASTER;

import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.ImmutablePropertyAccessor;
import org.sagebionetworks.PropertyAccessor;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCountingSemaphore;
import org.sagebionetworks.repo.model.dbo.persistence.DBOLockMaster;
import org.sagebionetworks.repo.model.exception.LockReleaseFailedException;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Basic database backed implementation of the CountingSemaphoreDao.
 * 
 */
public class CountingSemaphoreDaoImpl implements CountingSemaphoreDao, BeanNameAware {

	private static final String SQL_COUNT_LOCKS = "SELECT COUNT(*) FROM " + TABLE_COUNTING_SEMAPHORE + " WHERE " + COL_COUNTING_SEMAPHORE_KEY
			+ " = ?";
	private static final String SQL_FORCE_RELEASE_EXPIRED_LOCKS = "DELETE FROM " + TABLE_COUNTING_SEMAPHORE + " WHERE "
			+ COL_COUNTING_SEMAPHORE_KEY + " = ? AND " + COL_COUNTING_SEMAPHORE_EXPIRES + " < ?";
	private static final String SQL_DELETE_ALL_LOCKS = "DELETE FROM " + TABLE_COUNTING_SEMAPHORE + " WHERE " + COL_COUNTING_SEMAPHORE_KEY
			+ " IS NOT NULL";
	private static final String SQL_SELECT_EXCLUSIVE_FOR_UPDATE = "SELECT " + COL_LOCK_MASTER_KEY + " FROM " + TABLE_LOCK_MASTER + " WHERE "
			+ COL_LOCK_MASTER_KEY + " = ? FOR UPDATE";
	private static final String SQL_RELEASE_LOCK = "DELETE FROM " + TABLE_COUNTING_SEMAPHORE + " WHERE " + COL_COUNTING_SEMAPHORE_KEY
			+ " = ? AND " + COL_COUNTING_SEMAPHORE_LOCK_TOKEN + " = ?";
	private static final String SQL_UPDATE_LOCK_TIMEOUT = "UPDATE " + TABLE_COUNTING_SEMAPHORE + " SET " + COL_COUNTING_SEMAPHORE_EXPIRES
			+ " = ? WHERE " + COL_COUNTING_SEMAPHORE_LOCK_TOKEN + " = ?";

	static private Logger log = LogManager.getLogger(CountingSemaphoreDaoImpl.class);

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Resource(name = "txManager")
	private PlatformTransactionManager transactionManager;

	private long lockTimeoutMS;
	private String key;
	private PropertyAccessor<Integer> maxCount = null;
	private TransactionTemplate transactionTemplate;

	@Required
	public void setLockTimeoutMS(long lockTimeoutMS) {
		if (lockTimeoutMS < 300) {
			throw new IllegalArgumentException("lockTimeoutMS should be greater than 300 ms");
		}
		this.lockTimeoutMS = lockTimeoutMS;
	}

	@Override
	public void setBeanName(String key) {
		if (StringUtils.isBlank(key)) {
			throw new IllegalArgumentException("bean name should not be an empty or blank string");
		}
		this.key = key;
	}

	public void setMaxCount(int maxCount) {
		if (maxCount < 1) {
			throw new IllegalArgumentException("maxCount should be 1 or more, but was: " + maxCount);
		}
		this.maxCount = ImmutablePropertyAccessor.create(maxCount);
	}

	public void setMaxCountAccessor(PropertyAccessor<Integer> maxCount) {
		this.maxCount = maxCount;
	}

	@PostConstruct
	public void init() {
		if (maxCount == null) {
			throw new IllegalArgumentException("either maxCount or maxCountAccessor is required");
		}
		if (StringUtils.isBlank(key)) {
			throw new IllegalArgumentException("bean name should be set");
		}
		createMaster(key);

		DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
		transactionDef.setReadOnly(false);
		transactionDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		transactionDef.setName("CountingSemaphoreDao");
		// This will manage transactions for calls that need it.
		transactionTemplate = new TransactionTemplate(this.transactionManager, transactionDef);
	}

	@Override
	public String attemptToAcquireLock() {
		return doAttemptToAcquireLock(null);
	}

	@Override
	public String attemptToAcquireLock(String extraKey) {
		return doAttemptToAcquireLock(extraKey);
	}

	@NewWriteTransaction
	@Override
	public void releaseLock(String token) {
		doReleaseLock(token, null);
	}

	@NewWriteTransaction
	@Override
	public void releaseLock(String token, String extraKey) {
		doReleaseLock(token, extraKey);
	}

	@NewWriteTransaction
	@Override
	public void extendLockLease(String token) throws NotFoundException {
		int count = jdbcTemplate.update(SQL_UPDATE_LOCK_TIMEOUT, clock.currentTimeMillis() + lockTimeoutMS, token);
		if (count == 0) {
			throw new NotFoundException("Lock for token " + token + " not found");
		}
	}

	@Override
	public long getLockTimeoutMS() {
		return lockTimeoutMS;
	}

	private String doAttemptToAcquireLock(String extraKey) {
		final String keyName = getKeyName(extraKey);
		for (int i = 0; i < 3; i++) {
			try {
				return this.transactionTemplate.execute(new TransactionCallback<String>() {
					@Override
					public String doInTransaction(TransactionStatus status) {
						// First lock on the exclusive table entry
						try {
							jdbcTemplate.queryForObject(SQL_SELECT_EXCLUSIVE_FOR_UPDATE, String.class, keyName);
						} catch (EmptyResultDataAccessException e) {
							// if the semaphore was removed from the table due to testing or the extraKey is a new one,
							// recreate the master here and retry
							createMaster(keyName);
							jdbcTemplate.queryForObject(SQL_SELECT_EXCLUSIVE_FOR_UPDATE, String.class, keyName);
						}

						// Count the number of current locks
						long count = countOutstandingNonExpiredLocks(keyName);
						if (count >= maxCount.get()) {
							// all locks taken!
							return null;
						}

						DBOCountingSemaphore shared = new DBOCountingSemaphore();
						shared.setKey(keyName);
						shared.setToken(UUID.randomUUID().toString());
						shared.setExpires(clock.currentTimeMillis() + lockTimeoutMS);
						basicDao.createNew(shared);
						return shared.getToken();
					}
				});
			} catch (TransientDataAccessException e) {
				// sleep a bit with some random time, to avoid pounding the db
				clock.sleepNoInterrupt(50 + RandomUtils.nextInt(25));
			}
		}
		// could not acquire due to too many deadlocks
		return null;
	}

	private void doReleaseLock(String token, String extraKey) {
		String keyName = getKeyName(extraKey);
		int update = jdbcTemplate.update(SQL_RELEASE_LOCK, keyName, token);
		if (update < 1) {
			throw new LockReleaseFailedException("Failed to release the lock for key: " + keyName + " and token: " + token
					+ ".  Expired locks can be forcibly removed.");
		}
	}

	private String getKeyName(String extraKey) {
		String keyName = key;
		if (extraKey != null) {
			keyName = key + ":" + extraKey;
			if (keyName.length() >= DBOCountingSemaphore.KEY_NAME_LENGTH) {
				throw new IllegalArgumentException("key too long: " + keyName);
			}
		}
		return keyName;
	}

	private void createMaster(String keyName) {
		// create the exclusive lock entry
		DBOLockMaster lockMaster = new DBOLockMaster();
		lockMaster.setKey(keyName);
		try {
			log.info("Creating counting semaphore " + keyName);
			basicDao.createNew(lockMaster);
		} catch (IllegalArgumentException e) {
			if (e.getCause() != null && e.getCause().getClass() != DuplicateKeyException.class) {
				// we expect this one to happen on restart or during testing
				throw e;
			}
		}
	}

	private long countOutstandingNonExpiredLocks(String keyName) {
		// First delete all expired shared locks for this resource.
		this.jdbcTemplate.update(SQL_FORCE_RELEASE_EXPIRED_LOCKS, keyName, clock.currentTimeMillis());
		// Now count the remaining locks
		return this.jdbcTemplate.queryForObject(SQL_COUNT_LOCKS, Long.class, keyName);
	}

	public void forceReleaseAllLocks() {
		// Force the delete of all locks
		// This is basically a truncate table with cascade deletes cleaning up the read locks.
		this.jdbcTemplate.update(SQL_DELETE_ALL_LOCKS);
	}
}
