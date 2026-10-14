package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_DEP_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_DEP_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_DEP_OBJECT_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_DEP_SOURCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_DEP_SOURCE_TABLE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DEFINING_SQL_DEPENDENCY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DEFINING_SQL_DEPENDENCY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

/**
 * Maps a defining-SQL object (a materialized view or search index, identified by its node id and
 * {@code OBJECT_TYPE}) to a source table/view it depends on. Used as a reverse lookup so that when a
 * source table becomes available the dependent objects of a given type can be found and rebuilt.
 * <p>
 * Generalized from the materialized-view-only table. Prior production backups serialize the legacy
 * field names {@code materializedViewId} / {@code materializedViewVersion}; the translator bridges
 * those into the new {@code objectId} / {@code objectVersion} fields and defaults {@code objectType}
 * to {@link ObjectType#MATERIALIZED_VIEW}.
 */
public class DBODefiningSqlDependency
		implements MigratableDatabaseObject<DBODefiningSqlDependency, DBODefiningSqlDependency> {

	private static final String MATERIALIZED_VIEW_TYPE = ObjectType.MATERIALIZED_VIEW.name();

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("objectId", COL_DEFINING_SQL_DEP_OBJECT_ID, true).withIsBackupId(true),
		new FieldColumn("objectVersion", COL_DEFINING_SQL_DEP_OBJECT_VERSION, true),
		new FieldColumn("objectType", COL_DEFINING_SQL_DEP_OBJECT_TYPE),
		new FieldColumn("sourceTableId", COL_DEFINING_SQL_DEP_SOURCE_TABLE_ID, true),
		new FieldColumn("sourceTableVersion", COL_DEFINING_SQL_DEP_SOURCE_TABLE_VERSION, true)
	};

	// The both-non-null guard is the forcing function for removal: it can only fire if the bridge is
	// still present while a backup carries both the old and new column names, turning a stale bridge
	// into a hard failure rather than silent data corruption.
	@TemporaryCode(author = "BryanFauble", comment = "Remove with the materializedViewId/materializedViewVersion bridge fields once production serializes the objectId/objectVersion column names; this translator then becomes the identity.")
	private static final MigratableTableTranslation<DBODefiningSqlDependency, DBODefiningSqlDependency> TRANSLATOR = new BasicMigratableTableTranslation<DBODefiningSqlDependency>() {
		@Override
		public DBODefiningSqlDependency createDatabaseObjectFromBackup(DBODefiningSqlDependency dbo) {
			if (dbo.getObjectId() != null && dbo.getMaterializedViewId() != null) {
				throw new IllegalStateException(
						"Both objectId and the legacy materializedViewId are populated; the temporary migration bridge must be removed.");
			}
			if (dbo.getObjectVersion() != null && dbo.getMaterializedViewVersion() != null) {
				throw new IllegalStateException(
						"Both objectVersion and the legacy materializedViewVersion are populated; the temporary migration bridge must be removed.");
			}
			if (dbo.getObjectId() == null && dbo.getMaterializedViewId() != null) {
				dbo.setObjectId(dbo.getMaterializedViewId());
			}
			if (dbo.getObjectVersion() == null && dbo.getMaterializedViewVersion() != null) {
				dbo.setObjectVersion(dbo.getMaterializedViewVersion());
			}
			if (dbo.getObjectType() == null) {
				dbo.setObjectType(MATERIALIZED_VIEW_TYPE);
			}
			return dbo;
		}
	};

	private static final TableMapping<DBODefiningSqlDependency> TABLE_MAPPER = new TableMapping<DBODefiningSqlDependency>() {

		@Override
		public DBODefiningSqlDependency mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODefiningSqlDependency dbo = new DBODefiningSqlDependency();
			dbo.setObjectId(rs.getLong(COL_DEFINING_SQL_DEP_OBJECT_ID));
			dbo.setObjectVersion(rs.getLong(COL_DEFINING_SQL_DEP_OBJECT_VERSION));
			dbo.setObjectType(rs.getString(COL_DEFINING_SQL_DEP_OBJECT_TYPE));
			dbo.setSourceTableId(rs.getLong(COL_DEFINING_SQL_DEP_SOURCE_TABLE_ID));
			dbo.setSourceTableVersion(rs.getLong(COL_DEFINING_SQL_DEP_SOURCE_TABLE_VERSION));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DEFINING_SQL_DEPENDENCY;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DEFINING_SQL_DEPENDENCY;
		}

		@Override
		public Class<? extends DBODefiningSqlDependency> getDBOClass() {
			return DBODefiningSqlDependency.class;
		}
	};

	private Long objectId;
	private Long objectVersion;
	private String objectType;
	private Long sourceTableId;
	private Long sourceTableVersion;

	// Bridge fields: prior production backups serialize these legacy names. They have no FieldColumn
	// (not read from or written to the database) and are copied into objectId/objectVersion by the
	// translator during migration restore.
	@TemporaryCode(author = "BryanFauble", comment = "Remove once production serializes the objectId/objectVersion column names (see TRANSLATOR).")
	private Long materializedViewId;
	@TemporaryCode(author = "BryanFauble", comment = "Remove once production serializes the objectId/objectVersion column names (see TRANSLATOR).")
	private Long materializedViewVersion;

	public DBODefiningSqlDependency() {}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public Long getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getSourceTableId() {
		return sourceTableId;
	}

	public void setSourceTableId(Long sourceTableId) {
		this.sourceTableId = sourceTableId;
	}

	public Long getSourceTableVersion() {
		return sourceTableVersion;
	}

	public void setSourceTableVersion(Long sourceTableVersion) {
		this.sourceTableVersion = sourceTableVersion;
	}

	/**
	 * @deprecated Use {@link #getObjectId()}. Kept for migration backup compatibility.
	 */
	@Deprecated
	public Long getMaterializedViewId() {
		return materializedViewId;
	}

	/**
	 * @deprecated Use {@link #setObjectId(Long)}. Kept for migration backup compatibility.
	 */
	@Deprecated
	public void setMaterializedViewId(Long materializedViewId) {
		this.materializedViewId = materializedViewId;
	}

	/**
	 * @deprecated Use {@link #getObjectVersion()}. Kept for migration backup compatibility.
	 */
	@Deprecated
	public Long getMaterializedViewVersion() {
		return materializedViewVersion;
	}

	/**
	 * @deprecated Use {@link #setObjectVersion(Long)}. Kept for migration backup compatibility.
	 */
	@Deprecated
	public void setMaterializedViewVersion(Long materializedViewVersion) {
		this.materializedViewVersion = materializedViewVersion;
	}

	@Override
	public TableMapping<DBODefiningSqlDependency> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MATERIALIZED_VIEW_SOURCE_TABLE;
	}

	@Override
	public MigratableTableTranslation<DBODefiningSqlDependency, DBODefiningSqlDependency> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBODefiningSqlDependency> getBackupClass() {
		return DBODefiningSqlDependency.class;
	}

	@Override
	public Class<? extends DBODefiningSqlDependency> getDatabaseObjectClass() {
		return DBODefiningSqlDependency.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, objectVersion, objectType, sourceTableId, sourceTableVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBODefiningSqlDependency other = (DBODefiningSqlDependency) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(objectVersion, other.objectVersion)
				&& Objects.equals(objectType, other.objectType) && Objects.equals(sourceTableId, other.sourceTableId)
				&& Objects.equals(sourceTableVersion, other.sourceTableVersion);
	}

	@Override
	public String toString() {
		return "DBODefiningSqlDependency [objectId=" + objectId + ", objectVersion=" + objectVersion + ", objectType="
				+ objectType + ", sourceTableId=" + sourceTableId + ", sourceTableVersion=" + sourceTableVersion + "]";
	}

}
