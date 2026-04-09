WITH replica_clock (rep_id, seq) AS (
  VALUES
    %s
)
SELECT
  gp.SESSION_ID,
  gp.PATCH_ID_REP,
  gp.PATCH_ID_SEQ,
  gp.CREATED_ON,
  gp.EXPIRES_ON,
  gp.S3_KEY,
  gp.SIZE_BYTES
FROM
  GRID_PATCH AS gp
LEFT JOIN
  replica_clock AS rc ON gp.PATCH_ID_REP = rc.rep_id
WHERE
	gp.SESSION_ID = ?
	AND (rc.seq IS NULL OR gp.PATCH_ID_SEQ >= rc.seq)
ORDER BY
  gp.PATCH_ID_SEQ, gp.PATCH_ID_REP LIMIT ?;