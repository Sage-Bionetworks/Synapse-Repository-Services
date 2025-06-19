WITH replica_clock (rep_id, seq) AS (
  VALUES
    %s
)
SELECT
  gp.PATCH_ID_REP,
  gp.PATCH_ID_SEQ
FROM
  GRID_PATCH AS gp
LEFT JOIN
  replica_clock AS rc ON gp.PATCH_ID_REP = rc.rep_id
WHERE
	gp.SESSION_ID = ?
	AND (rc.seq IS NULL OR gp.PATCH_ID_SEQ > rc.seq)
ORDER BY
  gp.PATCH_ID_REP, gp.PATCH_ID_SEQ LIMIT ?;