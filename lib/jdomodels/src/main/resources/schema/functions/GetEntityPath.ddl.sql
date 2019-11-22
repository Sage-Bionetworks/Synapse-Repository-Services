CREATE FUNCTION `getEntityPath`(inputEntityId BIGINT) RETURNS varchar(1000) CHARSET utf8mb4
    READS SQL DATA
BEGIN
 	DECLARE entityId BIGINT;
	DECLARE parentId BIGINT;
    DECLARE counter BIGINT;
    DECLARE result VARCHAR(1000);
    DECLARE delimit CHAR(1);
    
    Set counter = 0;
    SET entityId = inputEntityId;
    SET delimit = '';
    SET result = '';
    WHILE entityId IS NOT NULL DO
    	SET parentId = NULL;
    	/* Lookup the parent of the current entity*/
    	SELECT PARENT_ID INTO parentId FROM JDONODE WHERE ID = entityId;
    	/* Done when the parentId is null*/
        IF parentId IS NULL THEN
        return result;
        END IF;
        IF counter > 0 THEN
        SET delimit = ',';
        END IF;
        SELECT CONCAT(result,delimit,parentId) INTO result;
        
    	/* Check the parent */
		SET entityId = parentId;
		/* Prevent an infinite loop */
		SET counter = counter + 1;
		IF counter > 100 THEN RETURN -1;
		END IF;
    END WHILE;
    RETURN result; 
 END