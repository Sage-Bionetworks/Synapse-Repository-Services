package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAP_ELIG_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAP_ELIG_PATCH_ID_REP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAP_ELIG_PATCH_ID_SEQ;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAP_ELIG_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAP_ELIG_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_SNAPSHOT_ELIGIBLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_SNAPSHOT_ELIGIBLE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Marks a grid session as eligible for snapshots on this stack: it records a patch that this stack saved, written
 * by {@link GridDao#markSnapshotEligible(String, org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp, String)}
 * after the patch commits.
 * <p>
 * Deliberately not migratable: the marker must describe this stack only, so a staging stack never snapshots a
 * session it only rebuilt or received through migration.
 */
public class DBOGridSnapshotEligible implements DatabaseObject<DBOGridSnapshotEligible> {

	private String sessionId;
	private Long patchIdRep;
	private Long patchIdSeq;
	private String s3Key;
	private Timestamp createdOn;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("sessionId", COL_GRID_SNAP_ELIG_SESSION_ID, true),
			new FieldColumn("patchIdRep", COL_GRID_SNAP_ELIG_PATCH_ID_REP),
			new FieldColumn("patchIdSeq", COL_GRID_SNAP_ELIG_PATCH_ID_SEQ),
			new FieldColumn("s3Key", COL_GRID_SNAP_ELIG_S3_KEY),
			new FieldColumn("createdOn", COL_GRID_SNAP_ELIG_CREATED_ON), };

	@Override
	public TableMapping<DBOGridSnapshotEligible> getTableMapping() {
		return new TableMapping<DBOGridSnapshotEligible>() {

			@Override
			public DBOGridSnapshotEligible mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridSnapshotEligible dbo = new DBOGridSnapshotEligible();
				dbo.setSessionId(rs.getString(COL_GRID_SNAP_ELIG_SESSION_ID));
				dbo.setPatchIdRep(rs.getLong(COL_GRID_SNAP_ELIG_PATCH_ID_REP));
				dbo.setPatchIdSeq(rs.getLong(COL_GRID_SNAP_ELIG_PATCH_ID_SEQ));
				dbo.setS3Key(rs.getString(COL_GRID_SNAP_ELIG_S3_KEY));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_SNAP_ELIG_CREATED_ON));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_SNAPSHOT_ELIGIBLE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_SNAPSHOT_ELIGIBLE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOGridSnapshotEligible> getDBOClass() {
				return DBOGridSnapshotEligible.class;
			}
		};
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getPatchIdRep() {
		return patchIdRep;
	}

	public void setPatchIdRep(Long patchIdRep) {
		this.patchIdRep = patchIdRep;
	}

	public Long getPatchIdSeq() {
		return patchIdSeq;
	}

	public void setPatchIdSeq(Long patchIdSeq) {
		this.patchIdSeq = patchIdSeq;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, patchIdRep, patchIdSeq, s3Key, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridSnapshotEligible other = (DBOGridSnapshotEligible) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(patchIdRep, other.patchIdRep)
				&& Objects.equals(patchIdSeq, other.patchIdSeq) && Objects.equals(s3Key, other.s3Key)
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "DBOGridSnapshotEligible [sessionId=" + sessionId + ", patchIdRep=" + patchIdRep + ", patchIdSeq="
				+ patchIdSeq + ", s3Key=" + s3Key + ", createdOn=" + createdOn + "]";
	}

}
