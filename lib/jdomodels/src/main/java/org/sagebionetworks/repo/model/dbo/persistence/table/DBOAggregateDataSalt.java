package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGGREGATE_DATA_SALT_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGGREGATE_DATA_SALT_SALT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AGGREGATE_DATA_SALT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AGGREGATE_DATA_SALT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The secret salt of an aggregate data source. It keys the record key of each participant, and therefore
 * the noise of every count released about that source (PLFM-10146). A row is written once, on first use,
 * and never changes; it is migrated so that the noise stays the same across stacks.
 */
public class DBOAggregateDataSalt implements MigratableDatabaseObject<DBOAggregateDataSalt, DBOAggregateDataSalt> {

	private static final MigratableTableTranslation<DBOAggregateDataSalt, DBOAggregateDataSalt> TRANSLATOR = new BasicMigratableTableTranslation<>();
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("objectId", COL_AGGREGATE_DATA_SALT_OBJECT_ID, true).withIsBackupId(true),
		new FieldColumn("salt", COL_AGGREGATE_DATA_SALT_SALT)
	};

	private static final TableMapping<DBOAggregateDataSalt> TABLE_MAPPER = new TableMapping<DBOAggregateDataSalt>() {

		@Override
		public DBOAggregateDataSalt mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOAggregateDataSalt dbo = new DBOAggregateDataSalt();
			dbo.setObjectId(rs.getLong(COL_AGGREGATE_DATA_SALT_OBJECT_ID));
			dbo.setSalt(rs.getString(COL_AGGREGATE_DATA_SALT_SALT));
			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_AGGREGATE_DATA_SALT;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_AGGREGATE_DATA_SALT;
		}

		@Override
		public Class<? extends DBOAggregateDataSalt> getDBOClass() {
			return DBOAggregateDataSalt.class;
		}
	};

	private Long objectId;
	private String salt;

	public DBOAggregateDataSalt() {}

	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	@Override
	public TableMapping<DBOAggregateDataSalt> getTableMapping() {
		return TABLE_MAPPER;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AGGREGATE_DATA_SALT;
	}

	@Override
	public MigratableTableTranslation<DBOAggregateDataSalt, DBOAggregateDataSalt> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOAggregateDataSalt> getBackupClass() {
		return DBOAggregateDataSalt.class;
	}

	@Override
	public Class<? extends DBOAggregateDataSalt> getDatabaseObjectClass() {
		return DBOAggregateDataSalt.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, salt);
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
		DBOAggregateDataSalt other = (DBOAggregateDataSalt) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(salt, other.salt);
	}

	@Override
	public String toString() {
		// The salt is a secret, so it is left out.
		return "DBOAggregateDataSalt [objectId=" + objectId + "]";
	}

}
