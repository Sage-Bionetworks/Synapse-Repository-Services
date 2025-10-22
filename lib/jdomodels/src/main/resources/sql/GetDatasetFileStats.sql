WITH ALL_FILES AS (
    SELECT D_FILES.OWNER_NODE_ID AS ID, FILES.CONTENT_SIZE AS SIZE
    FROM NODE_REVISION AS D
    -- Unpack the JSON array of file references from each dataset
    JOIN JSON_TABLE(
        D.items,
        '$[*]' COLUMNS (
            id VARCHAR(255) PATH '$.entityId',
            version BIGINT PATH '$.versionNumber'
        )
    ) AS D_FILES_REF
    JOIN NODE_REVISION AS D_FILES ON (
        -- The items array might contain entity ids with the syn prefix
        D_FILES.OWNER_NODE_ID = CAST(REPLACE(D_FILES_REF.id, 'syn', '') AS UNSIGNED)
        AND D_FILES.NUMBER = D_FILES_REF.version
    )
    JOIN FILES ON (D_FILES.FILE_HANDLE_ID = FILES.ID)
    WHERE (D.OWNER_NODE_ID, D.NUMBER) IN (:datasetRefs)
    -- De-duplicate the id/version pairs in case datasets overlap
    GROUP BY D_FILES.OWNER_NODE_ID, D_FILES.NUMBER
) SELECT COUNT(ALL_FILES.ID) AS count, COALESCE(SUM(ALL_FILES.SIZE), 0) AS size FROM ALL_FILES;