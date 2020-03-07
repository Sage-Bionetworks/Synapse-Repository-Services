CREATE PROCEDURE reserveId(IN idToReserve BIGINT, IN typeName VARCHAR(256))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
    DECLARE typeLock VARCHAR(256);
	
	SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

	/* This call may not need to be in stored procedure. */
	SET @sql_text:=CONCAT('INSERT IGNORE INTO ',typeName,' (ID, CREATED_ON) VALUES (', idToReserve, ',NOW())');
	PREPARE stmt from @sql_text;
	EXECUTE stmt; 
    DEALLOCATE PREPARE stmt;
    
	SELECT idToReserve AS NEW_ID;
END