/* Function to find the first entity in an entity's hierarchy with an ACL.*/
CREATE FUNCTION `getEntityBenefactorId`(inputEntityId BIGINT) RETURNS bigint(20)
    READS SQL DATA
BEGIN
 	DECLARE entityId BIGINT;
	DECLARE parentId BIGINT;
    DECLARE benefactorId BIGINT;
    
    SET entityId = inputEntityId;
    WHILE entityId IS NOT NULL DO
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
    END WHILE;
 END;
