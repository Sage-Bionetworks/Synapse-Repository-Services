/*
* This SQL template requires that a temporary table exists that contains only the files from the user's download
* that the user has full download access to.  The temporary table is given the alias 'U'.  The temporary table 'U'
* drives the recursive query to lookup the project that each file belongs, and to gather basic information about
* each entity to generate the PRO table.
* Next we need to gather the file's size by joining with the FILES table.  However, since an item on a user's
* download list can either explicitly reference a file's version, or the 'current version' (indicated by version = -1)
* there are two separate queries to handle each case.  The CUR_VER table handles the case where 'current version' is
* used (version = -1), and the VER table handles the case where an explicit version number is used.  The union
* of both of these tables: VER_U is used for the final query.  Note: CUR_VER and VER cannot reference the temporary table 'U'
* because doing do results in a MySQL error: ERROR 1137: Can't reopen table: 'U'.  Therefore, both of these tables are
* built from the full user's download list, but the results are then filtered down to the sub-set that the user can
* download with the final inner join to PRO table.
* Any additional filtering or sorting can be appended to the end of this query.
*/
WITH
	PRO AS (
		WITH RECURSIVE PRO (ENTITY_ID, ENTITY_NAME, CREATED_BY, CREATED_ON, PROJECT_ID, PROJECT_NAME, NODE_TYPE, PARENT_ID, DEPTH) AS (
			SELECT N.ID, N.NAME, N.CREATED_BY, N.CREATED_ON, N.ID, N.NAME, N.NODE_TYPE, N.PARENT_ID, 1 AS DEPTH 
				FROM %S U JOIN JDONODE N ON (U.ENTITY_ID = N.ID)
			UNION DISTINCT 
            SELECT PRO.ENTITY_ID, PRO.ENTITY_NAME, PRO.CREATED_BY, PRO.CREATED_ON, N.ID, N.NAME, N.NODE_TYPE, N.PARENT_ID, PRO.DEPTH + 1 AS DEPTH FROM
				PRO JOIN JDONODE N ON (PRO.PARENT_ID = N.ID) WHERE PRO.NODE_TYPE <> 'project' AND PRO.DEPTH < :depth
		)
		SELECT ENTITY_ID, ENTITY_NAME, CREATED_BY, CREATED_ON, PROJECT_ID, PROJECT_NAME FROM PRO WHERE NODE_TYPE = 'project'
	),
    CUR_VER AS (
		SELECT D.*, R.FILE_HANDLE_ID, R.NUMBER AS ACTUAL_VERSION FROM DOWNLOAD_LIST_ITEM_V2 D
			JOIN JDONODE N ON (D.ENTITY_ID = N.ID) 
            JOIN JDOREVISION R ON (N.ID = R.OWNER_NODE_ID AND N.CURRENT_REV_NUM = R.NUMBER)
				WHERE D.VERSION_NUMBER = -1 AND D.PRINCIPAL_ID = :principalId
    ),
    VER AS (
		SELECT D.*, R.FILE_HANDLE_ID, R.NUMBER AS ACTUAL_VERSION FROM DOWNLOAD_LIST_ITEM_V2 D
            JOIN JDOREVISION R ON (D.ENTITY_ID = R.OWNER_NODE_ID AND D.VERSION_NUMBER = R.NUMBER)
				WHERE D.VERSION_NUMBER <> -1 AND D.PRINCIPAL_ID = :principalId
    ),
    VER_U AS (
		SELECT * FROM CUR_VER
        UNION ALL
        SELECT * FROM VER
    )
SELECT VER_U.*, F.CONTENT_SIZE, PRO.ENTITY_NAME, PRO.CREATED_BY, PRO.CREATED_ON, PRO.PROJECT_ID, PRO.PROJECT_NAME
	 FROM VER_U JOIN PRO ON (VER_U.ENTITY_ID = PRO.ENTITY_ID) JOIN FILES F ON (VER_U.FILE_HANDLE_ID = F.ID)