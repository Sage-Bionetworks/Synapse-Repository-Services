/*
 * Will attempt to release an existing lock.
 * 
 * This procedure manages it own transactions to guarantee that a slow-down from a caller
 * cannot extend the duration of its exclusive locks.  Therefore, it must be called from
 * a new database session (i.e. using Propagation.REQUIRES_NEW) to prevent the auto commit
 * of any existing transaction managed by the caller.  
 */
CREATE PROCEDURE releaseSemaphoreLock(IN tokenIn VARCHAR(256))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
	/*
	 * Note: We set the expires_on to be five minutes into the future to block garbage collection from
	 * immediately deleting this row.
	 */
    START TRANSACTION;
	UPDATE SEMAPHORE_LOCK SET TOKEN = NULL, CONTEXT = NULL, EXPIRES_ON = (NOW() + INTERVAL 5 MINUTE) WHERE TOKEN = tokenIn;
	SELECT ROW_COUNT() AS RESULT;
	COMMIT;
END