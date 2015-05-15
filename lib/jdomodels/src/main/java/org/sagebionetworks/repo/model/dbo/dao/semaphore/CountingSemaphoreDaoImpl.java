package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.ImmutablePropertyAccessor;
import org.sagebionetworks.PropertyAccessor;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Another abstraction over the multiple lock semaphore.
 * 
 */
public class CountingSemaphoreDaoImpl implements CountingSemaphoreDao, BeanNameAware {

	public static final int KEY_NAME_LENGTH = 100;

	@Autowired
	private MultipleLockSemaphore multipleLockSemaphore;

	private long lockTimeoutMS;
	private String key;
	private PropertyAccessor<Integer> maxCount = null;

	public void setLockTimeoutMS(long lockTimeoutMS) {
		if (lockTimeoutMS < 1000) {
			throw new IllegalArgumentException("lockTimeoutMS cannot be less than 1000 ms");
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
	}

	@Override
	public String attemptToAcquireLock() {
		return doAttemptToAcquireLock(null);
	}

	@Override
	public String attemptToAcquireLock(String extraKey) {
		return doAttemptToAcquireLock(extraKey);
	}

	@Override
	public void releaseLock(String token) {
		doReleaseLock(token, null);
	}

	@Override
	public void releaseLock(String token, String extraKey) {
		doReleaseLock(token, extraKey);
	}

	@Override
	public void extendLockLease(String token) throws NotFoundException {
		long timeoutSec = lockTimeoutMS/1000;
		multipleLockSemaphore.refreshLockTimeout(key, token, timeoutSec);
	}

	@Override
	public long getLockTimeoutMS() {
		return lockTimeoutMS;
	}

	private String doAttemptToAcquireLock(String extraKey) {
		final String keyName = getKeyName(extraKey);
		long timeoutSec = lockTimeoutMS/1000;
		return this.multipleLockSemaphore.attemptToAcquireLock(keyName, timeoutSec, maxCount.get());
	}

	private void doReleaseLock(String token, String extraKey) {
		String keyName = getKeyName(extraKey);
		this.multipleLockSemaphore.releaseLock(keyName, token);
	}

	private String getKeyName(String extraKey) {
		String keyName = key;
		if (extraKey != null) {
			keyName = key + ":" + extraKey;
			if (keyName.length() >= KEY_NAME_LENGTH) {
				throw new IllegalArgumentException("key too long: " + keyName);
			}
		}
		return keyName;
	}

	public void forceReleaseAllLocks() {
		this.multipleLockSemaphore.releaseAllLocks();
	}
}
