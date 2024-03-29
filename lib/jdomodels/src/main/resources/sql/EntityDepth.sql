WITH RECURSIVE D (DEPTH, PARENT_ID) AS
(
  SELECT 1 AS DEPTH, PARENT_ID FROM NODE WHERE ID = ?
  UNION
  SELECT D.DEPTH + 1, N.PARENT_ID FROM D JOIN NODE N ON (D.PARENT_ID = N.ID) 
  	WHERE D.PARENT_ID IS NOT NULL AND D.DEPTH < ?
)
SELECT MAX(DEPTH) AS MAX_DEPTH FROM D;