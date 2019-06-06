package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_VER_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_TRX_TO_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRX_TO_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOTransactionToVersion
		implements MigratableDatabaseObject<DBOTransactionToVersion, DBOTransactionToVersion> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("transactionId", COL_TABLE_TRX_TO_VER_TRX_ID, true).withIsBackupId(true),
			new FieldColumn("version", COL_TABLE_TRX_TO_VER_VER_NUM) };

	Long transactionId;
	Long version;

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TABLE_TRANSACTION_TO_VERSION;
	}

	@Override
	public TableMapping<DBOTransactionToVersion> getTableMapping() {
		return new TableMapping<DBOTransactionToVersion>() {

			@Override
			public DBOTransactionToVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTransactionToVersion dto = new DBOTransactionToVersion();
				dto.setTransactionId(rs.getLong(COL_TABLE_TRX_TO_VER_TRX_ID));
				dto.setVersion(rs.getLong(COL_TABLE_TRX_TO_VER_VER_NUM));
				return dto;
			}

			@Override
			public String getTableName() {
				return TABLE_TABLE_TRX_TO_VERSION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_TRX_TO_VERSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTransactionToVersion> getDBOClass() {
				return DBOTransactionToVersion.class;
			}
		};
	}

	@Override
	public MigratableTableTranslation<DBOTransactionToVersion, DBOTransactionToVersion> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOTransactionToVersion> getBackupClass() {
		return DBOTransactionToVersion.class;
	}

	@Override
	public Class<? extends DBOTransactionToVersion> getDatabaseObjectClass() {
		return DBOTransactionToVersion.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		DBOTransactionToVersion other = (DBOTransactionToVersion) obj;
		if (transactionId == null) {
			if (other.transactionId != null)
				return false;
		} else if (!transactionId.equals(other.transactionId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOTransactionToVersion [transactionId=" + transactionId + ", version=" + version + "]";
	}

}
