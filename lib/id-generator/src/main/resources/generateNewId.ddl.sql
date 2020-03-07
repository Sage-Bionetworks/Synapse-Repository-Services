CREATE PROCEDURE generateNewId(IN typeName VARCHAR(256))
    MODIFIES SQL DATA
    SQL SECURITY INVOKER
BEGIN
	DECLARE newId BIGINT;
    DECLARE typeLock VARCHAR(256);
	
	SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
 
 	/*
 	Note: MySQL does not require that the INSERT statement and the LAST_INSERT_ID()
 	be in the same transaction.  MySQL ties the LAST_INSERT_ID() call to the 
 	connection (not the transaction) so the same connection must be used for both calls.
 	However, the JdbcTemplate does not guarantee two calls will use the same connection 
 	unless both calls are in the same transaction. Therefore, we are using a stored 
 	procedure to guarantee both calls use the same connection WITHOUT using a transaction.
 	*/
	SET @sql_text:=CONCAT('INSERT INTO ',typeName,' (CREATED_ON) VALUES (NOW())');
	PREPARE stmt from @sql_text;
	EXECUTE stmt; 
    DEALLOCATE PREPARE stmt;
	
	SELECT LAST_INSERT_ID() as NEW_ID;
	
END