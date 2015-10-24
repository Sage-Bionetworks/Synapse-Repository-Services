package org.sagebionetworks.repo.model.jdo;

import java.util.logging.Logger;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

/**
 * This is a helper used to test that a lock can be Acquired and held on a node.
 * @author John
 *
 */
public class JDONodeLockCheckerImpl implements JDONodeLockChecker {
	
	private static Logger log = Logger.getLogger(JDONodeLockCheckerImpl.class.getName());
	
	@Autowired
	NodeDAO nodeDao;
	
	/**
	 * Note: This class is designed to be run in a multi-threaded environment and each
	 * of there variables MUST BE VOLATILE! 
	 * @see [http://www.javabeat.net/tips/169-volatile-keyword-in-java.html]
	 */
	private volatile boolean holdLock = true; 
	private volatile boolean lockAcquired = false;
	private volatile boolean failedDueToEtagConflict = false;
	private volatile String etag = "etag";
	private volatile long threadId = -1;
	private volatile long nodeId = -1;
	private volatile DatastoreException toThrow = null;

	@NewWriteTransaction
	@Override
	public void aquireAndHoldLock(String stringId, String currentETag) throws InterruptedException, NotFoundException, DatastoreException {
		holdLock = true;
		lockAcquired = false;
		nodeId = KeyFactory.stringToKey(stringId);
		etag = "";
		threadId = Thread.currentThread().getId();
		printStatusToLog();
		// Now try to acquire the lock
		try {
			etag = nodeDao.lockNodeAndIncrementEtag(stringId, currentETag);
		} catch (ConflictingUpdateException e) {
			// Failed to acquire the lock due to a conflict
			failedDueToEtagConflict = true;
			lockAcquired = false;
			holdLock = false;
			return;
		}
		lockAcquired = true;
		// Now hold the lock until told to release it
		while(holdLock){
			if(toThrow != null) {
				throw toThrow;
//				throw new RuntimeException(toThrow);
			};
			printStatusToLog();
			Thread.sleep(500);
		}
	}

	@Override
	public void releaseLock() {
		// release the lock
		holdLock = false;
		printStatusToLog();
	}

	@Override
	public boolean isLockAcquired() {
		printStatusToLog();
		return lockAcquired;
	}
	
	
	private void printStatusToLog(){
		log.info("Status: Thread ID: "+threadId+" acquired-lock: "+lockAcquired+" on Node: "+nodeId+", current eTag: "+etag+" holdlock: "+holdLock);
	}

	@Override
	public boolean failedDueToConflict() {
		return failedDueToEtagConflict;
	}

	@Override
	public String getEtag() {
		return etag;
	}

	@Override
	public void throwException(DatastoreException toThrow) {
		this.toThrow = toThrow;
	}
	

}
