/*
 * Ensure all of the rows needed for a lock exist.
 * 
 * This procedure manages it own transactions to guarantee that a slow-down from a caller
 * cannot extend the duration of its exclusive locks.  Therefore, it must be called from
 * a new database session (i.e. using Propagation.REQUIRES_NEW) to prevent the auto commit
 * of any existing transaction managed by the caller.  
 */
CREATE PROCEDURE bootstrapLockKeyRows(IN lockKey VARCHAR(256), IN maxLockCount INT(4))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
    DECLARE nextNumber TINYINT;
    DECLARE lockCount TINYINT;
	  
    /* Ensure the correct number of lock rows exist. */ 
    SELECT COUNT(LOCK_NUM) INTO lockCount FROM SEMAPHORE_LOCK WHERE LOCK_KEY = lockKey;
    IF lockCount < maxLockCount THEN
    	/* Unconditionally add all lock rows for this key.  See PLFM-5909. */
    	SET nextNumber = 0;
    	WHILE nextNumber < maxLockCount DO
    		/*
    		 * If two separate transactions attempt to bootstrap the same rows at the same time, the first transaction will
    		 * block the second transaction due to the exclusive locks used on an insert.  This means a slow-down in the
    		 * first transaction will spread to all other transactions. See: PLFM-8236.
    		 * By limiting the transaction size to a single insert, the blocking time is minimized.
    		 * Note: By setting the expires_on to be 5 minutes into the future, we block garbage collection from
    		 * removing the newly added rows.
    		 */
    		START TRANSACTION;
			INSERT IGNORE INTO SEMAPHORE_LOCK (LOCK_KEY, LOCK_NUM, TOKEN, EXPIRES_ON) VALUES 
				(lockKey, nextNumber, NULL, (NOW() + INTERVAL 5 MINUTE));
			COMMIT;
			SET nextNumber = nextNumber + 1;
		END WHILE;
	END IF;
END;