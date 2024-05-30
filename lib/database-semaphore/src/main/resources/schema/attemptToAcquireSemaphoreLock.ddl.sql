/**
 * This procedure manages it own transactions to guarantee that a slow-down from a caller
 * cannot extend the duration of its exclusive locks.  Therefore, it must be called from
 * a new database session (i.e. using Propagation.REQUIRES_NEW) to prevent the auto commit
 * of any existing transaction managed by the caller.  
 */
CREATE PROCEDURE attemptToAcquireSemaphoreLock(IN lockKey VARCHAR(256), IN timeoutSec INT(4), IN maxLockCount INT(4), IN inContext VARCHAR(256))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
	DECLARE newToken VARCHAR(256) DEFAULT NULL;
	DECLARE rowId MEDIUMINT DEFAULT NULL;
	    
    /* Ensure the lock rows exist for this key */
    CALL bootstrapLockKeyRows(lockKey, maxLockCount);
	
    START TRANSACTION;
	/* Find the first number for the given lock that has a null token or is expired. */
	SELECT ROW_ID INTO rowId FROM SEMAPHORE_LOCK WHERE LOCK_KEY = lockKey AND LOCK_NUM < maxLockCount
		AND (TOKEN IS NULL OR EXPIRES_ON < current_timestamp) LIMIT 1 FOR UPDATE SKIP LOCKED;
	
    /* Claim this number and issue a token */
	IF rowId IS NOT NULL THEN
		SET newToken = UUID();
        UPDATE SEMAPHORE_LOCK SET TOKEN = newToken, EXPIRES_ON = (CURRENT_TIMESTAMP + INTERVAL timeoutSec SECOND),
        		CONTEXT = inContext
        	WHERE ROW_ID = rowId;
	END IF;
	
	COMMIT;
	/* Return the new token if acquired */
	SELECT newToken AS TOKEN;
END;