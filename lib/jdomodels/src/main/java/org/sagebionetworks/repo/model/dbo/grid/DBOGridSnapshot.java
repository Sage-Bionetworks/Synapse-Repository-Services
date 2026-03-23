package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CLOCK_TABLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_S3_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SNAPSHOT_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_SNAPSHOT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_SNAPSHOT;

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

public class DBOGridSnapshot implements MigratableDatabaseObject<DBOGridSnapshot, DBOGridSnapshot> {

	private Long id;
	private String sessionId;
	private String clockTable;
	private Timestamp createdOn;
	private Long createdBy;
	private String s3Key;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_GRID_SNAPSHOT_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("sessionId", COL_GRID_SNAPSHOT_SESSION_ID),
			new FieldColumn("clockTable", COL_GRID_SNAPSHOT_CLOCK_TABLE),
			new FieldColumn("createdBy", COL_GRID_SNAPSHOT_CREATED_BY),
			new FieldColumn("createdOn", COL_GRID_SNAPSHOT_CREATED_ON),
			new FieldColumn("s3Key", COL_GRID_SNAPSHOT_S3_KEY), };
	
	@Override
	public TableMapping<DBOGridSnapshot> getTableMapping() {

		return new TableMapping<DBOGridSnapshot>() {

			@Override
			public DBOGridSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridSnapshot dbo = new DBOGridSnapshot();
				dbo.setId(rs.getLong(COL_GRID_SNAPSHOT_ID));
				dbo.setSessionId(rs.getString(COL_GRID_SNAPSHOT_SESSION_ID));
				dbo.setClockTable(rs.getString(COL_GRID_SNAPSHOT_CLOCK_TABLE));
				dbo.setCreatedBy(rs.getLong(COL_GRID_SNAPSHOT_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_SNAPSHOT_CREATED_ON));
				dbo.setS3Key(rs.getString(COL_GRID_SNAPSHOT_S3_KEY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_SNAPSHOT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_SNAPSHOT;
			}

			@Override
			public Class<? extends DBOGridSnapshot> getDBOClass() {
				return DBOGridSnapshot.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GRID_SNAPSHOT;
	}

	@Override
	public MigratableTableTranslation<DBOGridSnapshot, DBOGridSnapshot> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOGridSnapshot> getBackupClass() {
		return DBOGridSnapshot.class;
	}

	@Override
	public Class<? extends DBOGridSnapshot> getDatabaseObjectClass() {
		return DBOGridSnapshot.class;
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

	public String getClockTable() {
		return clockTable;
	}

	public void setClockTable(String clockTable) {
		this.clockTable = clockTable;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getS3Key() {
		return s3Key;
	}

	public void setS3Key(String s3Key) {
		this.s3Key = s3Key;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, id, clockTable, createdBy, s3Key, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridSnapshot other = (DBOGridSnapshot) obj;
		return Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(id, other.id) && Objects.equals(clockTable, other.clockTable)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(s3Key, other.s3Key)
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "DBOGridSnapshot [id=" + id + ", sessionId=" + sessionId + ", clockTable=" + clockTable + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", s3Key=" + s3Key + "]";
	}

}
