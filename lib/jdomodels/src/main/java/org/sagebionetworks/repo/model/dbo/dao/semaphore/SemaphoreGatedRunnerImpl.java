package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.ImmutablePropertyAccessor;
import org.sagebionetworks.PropertyAccessor;
import org.sagebionetworks.collections.Maps2;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.model.dao.semaphore.ProgressingRunner;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreGatedRunner;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Supplier;
import com.google.common.collect.Sets;

/**
 * This implementation uses a database backed semaphore and designed to be used with a 
 * cluster of runners.  So when one machine acquires a lock, that lock cannot be acquired by any other 
 * machine in the cluster.  This guarantees that the total number of concurrent runner across the 
 * entire cluster never exceeds the configured maximum number of runners.  Since each machine in the 
 * cluster attempts to acquire locks at a regular interval, lock acquisition should be equally distributed 
 * across the cluster on average (assuming all machines are healthy and responsive).  This is important 
 * because it means we can add more worker machines to the cluster to reduce the load across the entire cluster.
 * For example, if the maximum number of runners is set to 10, and there is only one machine in the cluster,
 * then the one machine will be holding all 10 lock at any given time.  If another machine is added to the 
 * cluster, the second machine will start to acquire some of the locks. Over time, a new equilibrium should
 * be reach where each machine is holding 5 locks at any given time, so the work should be equally divided
 * across the cluster.
 * 
 * @author John
 *
 */
public class SemaphoreGatedRunnerImpl implements SemaphoreGatedRunner {
	
	static private Logger log = LogManager.getLogger(SemaphoreGatedRunnerImpl.class);
	
	/**
	 * This set ensures that the same key is not used by two separate runners
	 */
	private static Map<Object, Set<String>> USED_KEY_SET = Maps2.createSupplierHashMap(new Supplier<Set<String>>() {
		@Override
		public Set<String> get() {
			return Sets.newHashSet();
		}
	});
	/**
	 * The maximum number of characters allowed for a semaphore key. 
	 */
	public static int MAX_KEY_LENGTH = 30;
	/**
	 * The lock timeout cannot be set to less then 10 seconds. 
	 */
	public static long MIN_TIMEOUT_MS = 10*1000;
	
	@Autowired
	private CountingSemaphore countingSemaphore;
	private String semaphoreKey;
	private PropertyAccessor<Integer> maxNumberRunners;
	private Object runner;
	private long timeoutMS;

	@Autowired
	private Clock clock;
	
	/**
	 * Used for mock testing.
	 * 
	 * @param semaphoreDao
	 */
	public void setSemaphoreDao(CountingSemaphore countingSemaphore) {
		this.countingSemaphore = countingSemaphore;
	}

	/**
	 * Injected via Spring.
	 * @param semaphoreKey
	 */
	public void setSemaphoreKey(String semaphoreKey) {
		if(semaphoreKey == null) throw new IllegalArgumentException("semaphoreKey cannot be null");
		if(semaphoreKey.length() > MAX_KEY_LENGTH) throw new IllegalArgumentException("semaphoreKey cannot be longer than "+MAX_KEY_LENGTH+" characters");
		// This checks to make sure that we don't use the same key twice. For testing, we reload the context multiple
		// times, which leads to these beans being recreated multiple times. This check uses a singleton bean to make
		// sure we only check for duplicates within a single bean context and not across bean contexts.
		if (!USED_KEY_SET.get(countingSemaphore).add(semaphoreKey)) {
			throw new IllegalArgumentException("The key: '" + semaphoreKey + "' is already in use. Duplicate key name?");
		}
		this.semaphoreKey = semaphoreKey;
	}

	/**
	 * Injected via Spring
	 * @param maxNumberRunners The maximum number of runners of this type.  This gate will guarantee that there are never more than this number of 
	 * runners (inclusive) concurrently running across the entire cluster.  Set this to a number less than one to disable this runner.
	 */
	public void setMaxNumberRunners(int maxNumberRunners) {
		this.maxNumberRunners = ImmutablePropertyAccessor.create(maxNumberRunners);
	}

	/**
	 * Injected via Spring
	 * 
	 * @param maxNumberRunners The maximum number of runners of this type. This gate will guarantee that there are never
	 *        more than this number of runners (inclusive) concurrently running across the entire cluster. Set this to a
	 *        number less than one to disable this runner.
	 */
	public void setMaxNumberRunnersAccessor(PropertyAccessor<Integer> maxNumberRunners) {
		this.maxNumberRunners = maxNumberRunners;
	}

	/**
	 * Injected via Spring
	 * 
	 * @param runner When a lock is acquired, the run() of this runner will be called.
	 */
	public void setRunner(Object runner) {
		if(runner == null) throw new IllegalArgumentException("Runner cannot be null");
		if(!(runner instanceof Runnable)){
			if(!(runner instanceof ProgressingRunner)){
				throw new IllegalArgumentException("Runner must implement either  "+Runnable.class.getName()+" or "+ProgressingRunner.class.getName());
			}
		}
		this.runner = runner;
	}

	/**
	 * Injected via Spring
	 * @param timeoutMS
	 */
	public void setTimeoutMS(long timeoutMS) {
		if(timeoutMS < MIN_TIMEOUT_MS) throw new IllegalArgumentException("The lock timeout is below the minimum timeout of "+MIN_TIMEOUT_MS+" MS");
		this.timeoutMS = timeoutMS;
	}

	@Override
	public void attemptToRun() {
		if(this.semaphoreKey == null) throw new IllegalArgumentException("semaphoreKey cannot be null");
		if(this.countingSemaphore == null) throw new IllegalArgumentException("countingSemaphore cannot be null");
		if(this.runner == null) throw new IllegalArgumentException("Runner cannot be null");
		if(this.timeoutMS < MIN_TIMEOUT_MS) throw new IllegalArgumentException("The lock timeout is below the minimum timeout of "+MIN_TIMEOUT_MS+" MS");
		// do nothing if the max number of of runner is less than one
		if (maxNumberRunners.get() < 1) {
			if(log.isDebugEnabled()){
				log.debug("Max number of runners is less than one so the runner will not be run");
			}
			return;
		}
		// randomly generate a lock number to attempt
		final long timeoutSec = timeoutMS/1000L;
		final String token = countingSemaphore.attemptToAcquireLock(semaphoreKey, timeoutSec, maxNumberRunners.get());
		if(token != null){
			try{
				// Make a run
				if(runner instanceof Runnable){
					((Runnable)runner).run();
				}else if(runner instanceof ProgressingRunner){
					// This type of runner allows the lock timeout to be refreshed.
					ProgressingRunner progressingRunner = (ProgressingRunner) runner;
					progressingRunner.run(new HalfLifeProgressCallback(semaphoreKey, token));
				}else{
					throw new RuntimeException("Unknown runner type: "+runner.getClass().getName());
				}
				
			}catch(Exception e){
				log.error("runner failed: ", e);
			}finally{
				try {
					countingSemaphore.releaseLock(semaphoreKey, token);
				} catch (LockReleaseFailedException e) {
					log.info(e.getMessage());
				}
			}
		}
	}

	/**
	 * For test cleanup only.
	 */
	void clearKeys(){
		USED_KEY_SET.clear();
	}

	/**
	 * This callback will refresh the lock if half of the lock's timeout has 
	 * expired since the last rest.
	 * @author jmhill
	 *
	 */
	private class HalfLifeProgressCallback implements ProgressCallback<Void> {
		String key;
		String token;
		long halfExpirationTime;
		
		public HalfLifeProgressCallback(String key, String token) {
			super();
			this.key = key;
			this.token = token;
			resetHalfExpirationTime();
		}

		@Override
		public void progressMade(Void nothing) {
			// If past the half expired, then reset the timeout
			long now = clock.currentTimeMillis();
			if(now > halfExpirationTime){
				// Refresh the timer
				long timeoutSec = timeoutMS/1000L;
				countingSemaphore.refreshLockTimeout(key,token, timeoutSec);
				// Reset the local expiration time.
				resetHalfExpirationTime();
			}
			
		}
		
		/**
		 * The half-expiration time is now + timeout/2
		 */
		private void resetHalfExpirationTime(){
			halfExpirationTime = clock.currentTimeMillis() + timeoutMS / 2L;
		}
		
	}

	@Override
	public String getSemaphoreKey() {
		return semaphoreKey;
	}
}
