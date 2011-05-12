package org.sagebionetworks.repo.model.jdo;

import java.util.logging.Logger;

import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private volatile long etag = -1;
	private volatile long threadId = -1;
	private volatile long nodeId = -1;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	@Override
	public void aquireAndHoldLock(String stringId) throws InterruptedException, NotFoundException {
		holdLock = true;
		lockAcquired = false;
		nodeId = Long.parseLong(stringId);
		etag = -1;
		threadId = Thread.currentThread().getId();
		printStatusToLog();
		// Now try to acquire the lock
		etag = nodeDao.getETagForUpdate(stringId);
		lockAcquired = true;
		// Now hold the lock until told to release it
		while(holdLock){
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
		log.info("Status: Thread ID: "+threadId+" acquired-lock: "+lockAcquired+" on Node: "+nodeId+", current eTag: "+etag+"");
	}

}
