/*
 * Attempt to refresh an existing lock.
 * 
 * This procedure manages it own transactions to guarantee that a slow-down from a caller
 * cannot extend the duration of its exclusive locks.  Therefore, it must be called from
 * a new database session (i.e. using Propagation.REQUIRES_NEW) to prevent the auto commit
 * of any existing transaction managed by the caller.  
 */
CREATE PROCEDURE refreshSemaphoreLock(IN tokenIn VARCHAR(256), IN timeoutSec INT(4))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
    START TRANSACTION;
	UPDATE SEMAPHORE_LOCK SET EXPIRES_ON = (CURRENT_TIMESTAMP + INTERVAL timeoutSec SECOND) WHERE TOKEN = tokenIn;
	SELECT ROW_COUNT() AS RESULT;
	COMMIT;
END;