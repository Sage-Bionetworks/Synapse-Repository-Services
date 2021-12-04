package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_ID_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MV_ID_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_MV_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MV_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * This DBO represents the node identifier that the materialized view refers to. It is used as a primary migratable object
 * so that it is migrated independently from the node/revision tables
 */
public class DBOMaterializedViewId implements MigratableDatabaseObject<DBOMaterializedViewId, DBOMaterializedViewId> {
	
	public static final Long DEFAULT_VERSION = -1L;

	private static final List<MigratableDatabaseObject<?, ?>> SECONDARY_OBJECTS = Arrays.asList(new DBOMaterializedViewSourceTable());
	private static final MigratableTableTranslation<DBOMaterializedViewId, DBOMaterializedViewId> TRANSLATOR = new BasicMigratableTableTranslation<>();
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_MV_ID_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_MV_ID_ETAG).withIsEtag(true)
	};
	
	private static final TableMapping<DBOMaterializedViewId> TABLE_MAPPER = new TableMapping<DBOMaterializedViewId>() {
		
		@Override
		public DBOMaterializedViewId mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMaterializedViewId dbo = new DBOMaterializedViewId();
			dbo.setId(rs.getLong(COL_MV_ID_ID));
			dbo.setEtag(rs.getString(COL_MV_ID_ETAG));
			return dbo;
		}
		
		@Override
		public String getTableName() {
			return TABLE_MV_ID;
		}
		
		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}
		
		@Override
		public String getDDLFileName() {
			return DDL_MV_ID;
		}
		
		@Override
		public Class<? extends DBOMaterializedViewId> getDBOClass() {
			return DBOMaterializedViewId.class;
		}
	};
	
	private Long id;
	private String etag;
	
	public DBOMaterializedViewId() {}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public TableMapping<DBOMaterializedViewId> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MATERIALIZED_VIEW_ID;
	}

	@Override
	public MigratableTableTranslation<DBOMaterializedViewId, DBOMaterializedViewId> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOMaterializedViewId> getBackupClass() {
		return DBOMaterializedViewId.class;
	}

	@Override
	public Class<? extends DBOMaterializedViewId> getDatabaseObjectClass() {
		return DBOMaterializedViewId.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return SECONDARY_OBJECTS;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id);
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
		DBOMaterializedViewId other = (DBOMaterializedViewId) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "DBOMaterializedViewId [id=" + id + ", etag=" + etag + "]";
	}

}
