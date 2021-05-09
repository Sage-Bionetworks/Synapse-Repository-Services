/* Function to find the first entity in an entity's hierarchy with an ACL.*/
CREATE FUNCTION `getEntityBenefactorId`(inputEntityId BIGINT) RETURNS BIGINT
    READS SQL DATA
BEGIN
 	DECLARE entityId BIGINT;
	DECLARE parentId BIGINT;
    DECLARE benefactorId BIGINT;
    DECLARE counter BIGINT;
    
    Set counter = 0;
    SET entityId = inputEntityId;
    WHILE entityId IS NOT NULL DO
    	/* Fix for PLFM-4369: Start with null variables for each pass */
    	SET benefactorId = NULL;
    	SET parentId = NULL;
    	/* Does this entity have an ACL? */
    	SELECT OWNER_ID INTO benefactorId FROM ACL WHERE OWNER_ID = entityId AND OWNER_TYPE = 'ENTITY';
    	IF benefactorId IS NOT NULL THEN RETURN benefactorId;
    	END IF;
    	/*This entity does not have a benefactor so check its parent */
    	SELECT PARENT_ID INTO parentId FROM JDONODE WHERE ID = entityId;
    	/*If the parentID is null then a benefactor could not be found.*/
    	IF parentId IS NULL THEN RETURN NULL;
    	END IF;
    	/*Perform the same check on the parent*/
		SET entityId = parentId;
		/* Prevent an infinite loop */
		SET counter = counter + 1;
		IF counter > 51 THEN RETURN -1;
		END IF;
    END WHILE;
 END;
