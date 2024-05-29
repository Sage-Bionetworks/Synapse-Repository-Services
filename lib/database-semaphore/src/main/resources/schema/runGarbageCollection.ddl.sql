/*
 * Will delete any rows where the token is null and the expires_on is expired.
 * 
 * This procedure manages it own transactions to guarantee that a slow-down from a caller
 * cannot extend the duration of its exclusive locks.  Therefore, it must be called from
 * a new database session (i.e. using Propagation.REQUIRES_NEW) to prevent the auto commit
 * of any existing transaction managed by the caller.  
 */
CREATE PROCEDURE runGarbageCollection()
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN

	DECLARE rowId MEDIUMINT DEFAULT NULL;

	the_loop: LOOP
		/* 
		 * Find the and lock the first row that can be deleted.
		 */
	   	START TRANSACTION;
    	SELECT ROW_ID INTO rowId FROM SEMAPHORE_LOCK WHERE TOKEN IS NULL AND
			(now() > EXPIRES_ON) LIMIT 1 FOR UPDATE SKIP LOCKED;
		
		IF rowId IS NOT NULL THEN
			DELETE FROM SEMAPHORE_LOCK WHERE ROW_ID = rowId;
			COMMIT;
			SET rowId = NULL;
		ELSE
			/* 
			 * unable to find any more rows to delete so garbage collection is done.
			 */
			COMMIT;
			LEAVE the_loop;
		END IF;
	END LOOP the_loop;
END;