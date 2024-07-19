package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_LAST_ACCESSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PROJECT_STAT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STAT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOProjectStat implements MigratableDatabaseObject<DBOProjectStat, DBOProjectStat> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_PROJECT_STAT_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("projectId", COL_PROJECT_STAT_PROJECT_ID),
			new FieldColumn("userId", COL_PROJECT_STAT_USER_ID),
			new FieldColumn("lastAccessed", COL_PROJECT_STAT_LAST_ACCESSED),
			new FieldColumn("etag", COL_PROJECT_STAT_ETAG).withIsEtag(true),
	};

	private Long id;
	private Long projectId;
	private Long userId;
	private Long lastAccessed;
	private String etag;

	@Override
	public TableMapping<DBOProjectStat> getTableMapping() {
		return new TableMapping<DBOProjectStat>() {
			
			@Override
			public DBOProjectStat mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOProjectStat dbo = new DBOProjectStat();
				dbo.setId(rs.getLong(COL_PROJECT_STAT_ID));
				dbo.setProjectId(rs.getLong(COL_PROJECT_STAT_PROJECT_ID));
				dbo.setUserId(rs.getLong(COL_PROJECT_STAT_USER_ID));
				dbo.setLastAccessed(rs.getLong(COL_PROJECT_STAT_LAST_ACCESSED));
				dbo.setEtag(rs.getString(COL_PROJECT_STAT_ETAG));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_PROJECT_STAT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_PROJECT_STAT;
			}
			
			@Override
			public Class<? extends DBOProjectStat> getDBOClass() {
				return DBOProjectStat.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PROJECT_STATS;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(Long lastAccessed) {
		this.lastAccessed = lastAccessed;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id, lastAccessed, projectId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOProjectStat other = (DBOProjectStat) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(lastAccessed, other.lastAccessed) && Objects.equals(projectId, other.projectId)
				&& Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "DBOProjectStat [id=" + id + ", projectId=" + projectId + ", userId=" + userId + ", lastAccessed="
				+ lastAccessed + ", etag=" + etag + "]";
	}

	@Override
	public MigratableTableTranslation<DBOProjectStat, DBOProjectStat> getTranslator() {
		return new BasicMigratableTableTranslation<DBOProjectStat>();
	}

	@Override
	public Class<? extends DBOProjectStat> getBackupClass() {
		return DBOProjectStat.class;
	}

	@Override
	public Class<? extends DBOProjectStat> getDatabaseObjectClass() {
		return DBOProjectStat.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
