package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_MV_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_MV_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_SOURCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_TABLES_SOURCE_TABLE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_MV_SOURCE_TABLES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MV_TABLES;

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

public class DBOMaterializedViewSourceTable
		implements MigratableDatabaseObject<DBOMaterializedViewSourceTable, DBOMaterializedViewSourceTable> {

	private static final MigratableTableTranslation<DBOMaterializedViewSourceTable, DBOMaterializedViewSourceTable> TRANSLATOR = new BasicMigratableTableTranslation<>();
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("materializedViewId", COL_MV_TABLES_MV_ID, true).withIsBackupId(true),
		new FieldColumn("materializedViewVersion", COL_MV_TABLES_MV_VERSION, true),
		new FieldColumn("sourceTableId", COL_MV_TABLES_SOURCE_TABLE_ID, true),
		new FieldColumn("sourceTableVersion", COL_MV_TABLES_SOURCE_TABLE_VERSION, true) 
	};

	private static final TableMapping<DBOMaterializedViewSourceTable> TABLE_MAPPER = new TableMapping<DBOMaterializedViewSourceTable>() {

		@Override
		public DBOMaterializedViewSourceTable mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMaterializedViewSourceTable dbo = new DBOMaterializedViewSourceTable();
			dbo.setMaterializedViewId(rs.getLong(COL_MV_TABLES_MV_ID));
			dbo.setMaterializedViewVersion(rs.getLong(COL_MV_TABLES_MV_VERSION));
			dbo.setSourceTableId(rs.getLong(COL_MV_TABLES_SOURCE_TABLE_ID));
			dbo.setSourceTableVersion(rs.getLong(COL_MV_TABLES_SOURCE_TABLE_VERSION));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_MV_TABLES;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_MV_SOURCE_TABLES;
		}

		@Override
		public Class<? extends DBOMaterializedViewSourceTable> getDBOClass() {
			return DBOMaterializedViewSourceTable.class;
		}
	};

	private Long materializedViewId;
	private Long materializedViewVersion;
	private Long sourceTableId;
	private Long sourceTableVersion;

	public DBOMaterializedViewSourceTable() {}

	public Long getMaterializedViewId() {
		return materializedViewId;
	}

	public void setMaterializedViewId(Long materializedViewId) {
		this.materializedViewId = materializedViewId;
	}

	public Long getMaterializedViewVersion() {
		return materializedViewVersion;
	}

	public void setMaterializedViewVersion(Long materializedViewVersion) {
		this.materializedViewVersion = materializedViewVersion;
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

	@Override
	public TableMapping<DBOMaterializedViewSourceTable> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MATERIALIZED_VIEW_SOURCE_TABLE;
	}

	@Override
	public MigratableTableTranslation<DBOMaterializedViewSourceTable, DBOMaterializedViewSourceTable> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOMaterializedViewSourceTable> getBackupClass() {
		return DBOMaterializedViewSourceTable.class;
	}

	@Override
	public Class<? extends DBOMaterializedViewSourceTable> getDatabaseObjectClass() {
		return DBOMaterializedViewSourceTable.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(materializedViewId, materializedViewVersion, sourceTableId, sourceTableVersion);
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
		DBOMaterializedViewSourceTable other = (DBOMaterializedViewSourceTable) obj;
		return Objects.equals(materializedViewId, other.materializedViewId)
				&& Objects.equals(materializedViewVersion, other.materializedViewVersion)
				&& Objects.equals(sourceTableId, other.sourceTableId) && Objects.equals(sourceTableVersion, other.sourceTableVersion);
	}

	@Override
	public String toString() {
		return "DBOMaterializedViewSourceTable [materializedViewId=" + materializedViewId + ", materializedViewVersion="
				+ materializedViewVersion + ", sourceTableId=" + sourceTableId + ", sourceTableVersion=" + sourceTableVersion + "]";
	}

}
