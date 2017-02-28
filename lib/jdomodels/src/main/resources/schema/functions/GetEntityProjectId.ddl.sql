/* Function to find the first entity in an entity's hierarchy that is a Project*/
CREATE FUNCTION `getEntityProjectId`(inputEntityId BIGINT) RETURNS bigint(20)
    READS SQL DATA
BEGIN
 	DECLARE entityId BIGINT;
	DECLARE parentId BIGINT;
    DECLARE nodeType VARCHAR(30);
    
    SET entityId = inputEntityId;
    WHILE entityId IS NOT NULL DO
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
    END WHILE;
 END;
