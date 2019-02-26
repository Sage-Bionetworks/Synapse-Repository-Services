package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FORUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Data Binding Object for the Forum table
 * @author kimyentruong
 *
 */
public class DBOForum implements MigratableDatabaseObject<DBOForum, DBOForum>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_FORUM_ID, true).withIsBackupId(true),
		new FieldColumn("projectId", COL_FORUM_PROJECT_ID),
		new FieldColumn("etag", COL_FORUM_ETAG).withIsEtag(true)
	};

	private Long id;
	private Long projectId;
	private String etag;

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
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
		DBOForum other = (DBOForum) obj;
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
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOForum [id=" + id + ", projectId=" + projectId + ", etag=" + etag + "]";
	}

	@Override
	public TableMapping<DBOForum> getTableMapping() {
		return new TableMapping<DBOForum>() {

			@Override
			public DBOForum mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOForum dbo = new DBOForum();
				dbo.setId(rs.getLong(COL_FORUM_ID));
				dbo.setProjectId(rs.getLong(COL_FORUM_PROJECT_ID));
				dbo.setEtag(rs.getString(COL_FORUM_ETAG));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_FORUM;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FORUM;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOForum> getDBOClass() {
				return DBOForum.class;
			}
			
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FORUM;
	}

	@Override
	public MigratableTableTranslation<DBOForum, DBOForum> getTranslator() {
		return new MigratableTableTranslation<DBOForum, DBOForum>() {

			@Override
			public DBOForum createDatabaseObjectFromBackup(DBOForum backup) {
				if (backup.getEtag() == null) {
					backup.setEtag(UUID.randomUUID().toString());
				}
				return backup;
			}

			@Override
			public DBOForum createBackupFromDatabaseObject(DBOForum dbo) {
				if (dbo.getEtag() == null) {
					dbo.setEtag(UUID.randomUUID().toString());
				}
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOForum> getBackupClass() {
		return DBOForum.class;
	}

	@Override
	public Class<? extends DBOForum> getDatabaseObjectClass() {
		return DBOForum.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}
}
