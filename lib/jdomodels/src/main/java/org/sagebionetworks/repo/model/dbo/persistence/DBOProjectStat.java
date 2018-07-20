package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_LAST_ACCESSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STAT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_PROJECT_STAT, constraints = { "unique key UNIQUE_PSTAT_PID_USER (" + COL_PROJECT_STAT_PROJECT_ID + ", "
		+ COL_PROJECT_STAT_USER_ID + ")" })
public class DBOProjectStat implements MigratableDatabaseObject<DBOProjectStat, DBOProjectStat> {

	@Field(name = COL_PROJECT_STAT_ID, backupId = true, primary = true, nullable = false)
	private Long id;

	@Field(name = COL_PROJECT_STAT_PROJECT_ID, nullable = false)
	@ForeignKey(name = "PROJECT_STAT_PROJ_ID_FK", table = TABLE_NODE, field = COL_NODE_ID, cascadeDelete = true)
	private Long projectId;

	@Field(name = COL_PROJECT_STAT_USER_ID, nullable = false)
	@ForeignKey(name = "PROJECT_STAT_USR_ID_FK", table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long userId;

	@Field(name = COL_PROJECT_STAT_LAST_ACCESSED, nullable = false)
	private Date lastAccessed;
	
	@Field(name = COL_PROJECT_STAT_ETAG, etag = true, nullable = false)
	private String etag;

	private static TableMapping<DBOProjectStat> tableMapping = AutoTableMapping.create(DBOProjectStat.class);

	@Override
	public TableMapping<DBOProjectStat> getTableMapping() {
		return tableMapping;
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

	public Date getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(Date lastAccessed) {
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((lastAccessed == null) ? 0 : lastAccessed.hashCode());
		result = prime * result
				+ ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (lastAccessed == null) {
			if (other.lastAccessed != null)
				return false;
		} else if (!lastAccessed.equals(other.lastAccessed))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOProjectStat [id=" + id + ", projectId=" + projectId + ", userId=" + userId + ", lastAccessed=" + lastAccessed + "]";
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
