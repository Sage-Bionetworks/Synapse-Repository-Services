package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
		new FieldColumn("projectId", FK_FORUM_PROJECT_ID).withIsSelfForeignKey(true)
	};

	private Long id;
	private Long projectId;

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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((projectId == null) ? 0 : projectId.hashCode());
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
	public TableMapping<DBOForum> getTableMapping() {
		return new TableMapping<DBOForum>() {

			@Override
			public DBOForum mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOForum dbo = new DBOForum();
				dbo.setId(rs.getLong(COL_FORUM_ID));
				dbo.setProjectId(rs.getLong(FK_FORUM_PROJECT_ID));
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
				return backup;
			}

			@Override
			public DBOForum createBackupFromDatabaseObject(DBOForum dbo) {
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
