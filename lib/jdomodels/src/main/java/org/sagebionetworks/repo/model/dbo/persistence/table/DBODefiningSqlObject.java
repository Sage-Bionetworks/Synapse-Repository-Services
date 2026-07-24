package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_OBJECT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DEFINING_SQL_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_DEFINING_SQL_OBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DEFINING_SQL_OBJECT;

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
 * This DBO represents the node identifier of a defining-SQL object (e.g. a materialized view or a
 * search index). It is used as a primary migratable object so that it is migrated independently from
 * the node/revision tables, and it owns the {@link DBODefiningSqlDependency} secondary table.
 */
public class DBODefiningSqlObject implements MigratableDatabaseObject<DBODefiningSqlObject, DBODefiningSqlObject> {

	public static final Long DEFAULT_VERSION = -1L;

	private static final List<MigratableDatabaseObject<?, ?>> SECONDARY_OBJECTS = Arrays.asList(new DBODefiningSqlDependency());
	private static final MigratableTableTranslation<DBODefiningSqlObject, DBODefiningSqlObject> TRANSLATOR = new BasicMigratableTableTranslation<>();
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_DEFINING_SQL_OBJECT_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_DEFINING_SQL_OBJECT_ETAG).withIsEtag(true)
	};

	private static final TableMapping<DBODefiningSqlObject> TABLE_MAPPER = new TableMapping<DBODefiningSqlObject>() {

		@Override
		public DBODefiningSqlObject mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODefiningSqlObject dbo = new DBODefiningSqlObject();
			dbo.setId(rs.getLong(COL_DEFINING_SQL_OBJECT_ID));
			dbo.setEtag(rs.getString(COL_DEFINING_SQL_OBJECT_ETAG));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_DEFINING_SQL_OBJECT;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_DEFINING_SQL_OBJECT;
		}

		@Override
		public Class<? extends DBODefiningSqlObject> getDBOClass() {
			return DBODefiningSqlObject.class;
		}
	};

	private Long id;
	private String etag;

	public DBODefiningSqlObject() {}

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
	public TableMapping<DBODefiningSqlObject> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MATERIALIZED_VIEW_ID;
	}

	@Override
	public MigratableTableTranslation<DBODefiningSqlObject, DBODefiningSqlObject> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBODefiningSqlObject> getBackupClass() {
		return DBODefiningSqlObject.class;
	}

	@Override
	public Class<? extends DBODefiningSqlObject> getDatabaseObjectClass() {
		return DBODefiningSqlObject.class;
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
		DBODefiningSqlObject other = (DBODefiningSqlObject) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "DBODefiningSqlObject [id=" + id + ", etag=" + etag + "]";
	}

}
