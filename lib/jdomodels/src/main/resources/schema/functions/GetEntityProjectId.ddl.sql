/* Function to find the first entity in an entity's hierarchy that is a Project*/
CREATE FUNCTION `getEntityProjectId`(inputEntityId BIGINT) RETURNS BIGINT
    READS SQL DATA
BEGIN
 	DECLARE entityId BIGINT;
	DECLARE parentId BIGINT;
    DECLARE nodeType VARCHAR(30);
    DECLARE counter BIGINT;
    
    Set counter = 0;
    SET entityId = inputEntityId;
    WHILE entityId IS NOT NULL DO
        /* Fix for PLFM-4369: Start with null variables for each pass */
    	SET parentId = NULL;
    	SET nodeType = NULL;
    	/* Is this entity a project?*/
    	SELECT PARENT_ID, NODE_TYPE INTO parentId, nodeType FROM JDONODE WHERE ID = entityId;
    	/* If type is null then this entity does not exist so return null */
    	IF nodeType IS NULL THEN RETURN NULL;
    	/* If the node type is project then this entity is its own project*/
    	ELSEIF nodeType = 'project' THEN RETURN entityId;
    	/* If the parentId is null then a project could not be found so return null */
    	ELSEIF parentId IS NULL THEN RETURN NULL;
    	END IF;
    	/* Check the parent */
		SET entityId = parentId;
		/* Prevent an infinite loop */
		SET counter = counter + 1;
		IF counter > 51 THEN RETURN -1;
		END IF;
    END WHILE;
 END;
