package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FORUM_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FORUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FORUM;

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

/**
 * Data Binding Object for the Forum table
 * @author kimyentruong
 *
 */
public class DBOForum implements MigratableDatabaseObject<DBOForum, DBOForum>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_FORUM_ID, true).withIsBackupId(true),
		new FieldColumn("objectId", COL_FORUM_OBJECT_ID),
		new FieldColumn("objectType", COL_FORUM_OBJECT_TYPE),
		new FieldColumn("etag", COL_FORUM_ETAG).withIsEtag(true)
	};

	private Long id;
	// Temporary bridge field: old production backups serialize this field name.
	// The translator copies this into objectId during migration.
	private Long projectId;
	// The canonical field that maps to the OBJECT_ID column.
	private Long objectId;
	private String objectType;
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

	/**
	 * @deprecated Use {@link #getObjectId()} instead. Kept for migration backup compatibility.
	 */
	public Long getProjectId() {
		return projectId;
	}

	/**
	 * @deprecated Use {@link #setObjectId(Long)} instead. Kept for migration backup compatibility.
	 */
	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id, objectId, objectType);
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
		return Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id)
				&& Objects.equals(objectId, other.objectId)
				&& Objects.equals(objectType, other.objectType);
	}

	@Override
	public String toString() {
		return "DBOForum [id=" + id + ", objectId=" + objectId + ", objectType=" + objectType + ", etag=" + etag + "]";
	}

	@Override
	public TableMapping<DBOForum> getTableMapping() {
		return new TableMapping<DBOForum>() {

			@Override
			public DBOForum mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOForum dbo = new DBOForum();
				dbo.setId(rs.getLong(COL_FORUM_ID));
				dbo.setObjectId(rs.getLong(COL_FORUM_OBJECT_ID));
				dbo.setObjectType(rs.getString(COL_FORUM_OBJECT_TYPE));
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
		return new BasicMigratableTableTranslation<DBOForum>() {
			@Override
			public DBOForum createDatabaseObjectFromBackup(DBOForum dbo) {
				// Bridge: old backups have projectId but not objectId.
				// Copy projectId into objectId so it maps to the OBJECT_ID column.
				if (dbo.getObjectId() == null && dbo.getProjectId() != null) {
					dbo.setObjectId(dbo.getProjectId());
				}
				if (dbo.getObjectType() == null) {
					dbo.setObjectType("ENTITY");
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
