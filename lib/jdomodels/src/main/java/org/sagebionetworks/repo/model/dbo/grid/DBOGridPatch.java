package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_PATCH_ID_REP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_PATCH_ID_SEQ;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_PAT_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_PATCH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_PATCH;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOGridPatch implements MigratableDatabaseObject<DBOGridPatch, DBOGridPatch> {

	private Long id;
	private String sessionId;
	private Long patchIdRep;
	private Long patchIdSeq;
	private Timestamp createdOn;
	private Timestamp expiresOn;
	private String s3Key;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_GRID_PAT_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("sessionId", COL_GRID_PAT_SESSION_ID),
			new FieldColumn("patchIdRep", COL_GRID_PAT_PATCH_ID_REP),
			new FieldColumn("patchIdSeq", COL_GRID_PAT_PATCH_ID_SEQ),
			new FieldColumn("createdOn", COL_GRID_PAT_CREATED_ON),
			new FieldColumn("expiresOn", COL_GRID_PAT_EXPIRES_ON),
			new FieldColumn("s3Key", COL_GRID_PAT_S3_KEY), };
	
	@Override
	public TableMapping<DBOGridPatch> getTableMapping() {

		return new TableMapping<DBOGridPatch>() {

			@Override
			public DBOGridPatch mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridPatch dbo = new DBOGridPatch();
				dbo.setId(rs.getLong(COL_GRID_PAT_ID));
				dbo.setSessionId(rs.getString(COL_GRID_PAT_SESSION_ID));
				dbo.setPatchIdRep(rs.getLong(COL_GRID_PAT_PATCH_ID_REP));
				dbo.setPatchIdSeq(rs.getLong(COL_GRID_PAT_PATCH_ID_SEQ));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_PAT_CREATED_ON));
				dbo.setExpiresOn(rs.getTimestamp(COL_GRID_PAT_EXPIRES_ON));
				dbo.setS3Key(rs.getString(COL_GRID_PAT_S3_KEY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_PATCH;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_PATCH;
			}

			@Override
			public Class<? extends DBOGridPatch> getDBOClass() {
				return DBOGridPatch.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GRID_PATCH;
	}

	@Override
	public MigratableTableTranslation<DBOGridPatch, DBOGridPatch> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOGridPatch> getBackupClass() {
		return DBOGridPatch.class;
	}

	@Override
	public Class<? extends DBOGridPatch> getDatabaseObjectClass() {
		return DBOGridPatch.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Timestamp getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, expiresOn, id, patchIdRep, patchIdSeq, s3Key, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridPatch other = (DBOGridPatch) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(expiresOn, other.expiresOn)
				&& Objects.equals(id, other.id) && Objects.equals(patchIdRep, other.patchIdRep)
				&& Objects.equals(patchIdSeq, other.patchIdSeq) && Objects.equals(s3Key, other.s3Key)
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "DBOGridPatch [id=" + id + ", sessionId=" + sessionId + ", patchIdRep=" + patchIdRep + ", patchIdSeq="
				+ patchIdSeq + ", createdOn=" + createdOn + ", expiresOn=" + expiresOn + ", s3Key=" + s3Key + "]";
	}

}
