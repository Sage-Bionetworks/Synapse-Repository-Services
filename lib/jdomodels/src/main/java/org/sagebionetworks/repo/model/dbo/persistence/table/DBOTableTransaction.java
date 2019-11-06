package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TABLE_TRANSACTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRANSACTION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

import com.google.common.collect.Lists;

public class DBOTableTransaction implements MigratableDatabaseObject<DBOTableTransaction, DBOTableTransaction> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("transactionId", COL_TABLE_TRX_ID, true).withIsBackupId(true),
			new FieldColumn("tableId", COL_TABLE_TRX_TABLE_ID),
			new FieldColumn("startedBy", COL_TABLE_TRX_STARTED_BY),
			new FieldColumn("startedOn", COL_TABLE_TRX_STARTED_ON),
			new FieldColumn("etag", COL_TABLE_TRX_ETAG).withIsEtag(true)};

	Long transactionId;
	Long tableId;
	Long startedBy;
	Long startedOn;
	String etag;

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getStartedBy() {
		return startedBy;
	}

	public void setStartedBy(Long startedBy) {
		this.startedBy = startedBy;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public static FieldColumn[] getFields() {
		return FIELDS;
	}

	@Override
	public TableMapping<DBOTableTransaction> getTableMapping() {
		return new TableMapping<DBOTableTransaction>() {

			@Override
			public DBOTableTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTableTransaction dto = new DBOTableTransaction();
				dto.setTransactionId(rs.getLong(COL_TABLE_TRX_ID));
				dto.setTableId(rs.getLong(COL_TABLE_TRX_TABLE_ID));
				dto.setStartedOn(rs.getLong(COL_TABLE_TRX_STARTED_ON));
				dto.setStartedBy(rs.getLong(COL_TABLE_TRX_STARTED_BY));
				dto.setEtag(rs.getString(COL_TABLE_TRX_ETAG));
				return dto;
			}

			@Override
			public String getTableName() {
				return TABLE_TABLE_TRANSACTION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TABLE_TRANSACTION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTableTransaction> getDBOClass() {
				return DBOTableTransaction.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TABLE_TRANSACTION;
	}

	@Override
	public MigratableTableTranslation<DBOTableTransaction, DBOTableTransaction> getTranslator() {
		return new MigratableTableTranslation<DBOTableTransaction, DBOTableTransaction>(){

			@Override
			public DBOTableTransaction createDatabaseObjectFromBackup(DBOTableTransaction backup) {
				/*
				 *  This DBO did not start out with an etag.  The etag was added to support
				 *  linking a a transaction to a table's version.
				 */
				if(backup.getEtag() == null) {
					backup.setEtag(UUID.randomUUID().toString());
				}
				return backup;
			}

			@Override
			public DBOTableTransaction createBackupFromDatabaseObject(DBOTableTransaction dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOTableTransaction> getBackupClass() {
		return DBOTableTransaction.class;
	}

	@Override
	public Class<? extends DBOTableTransaction> getDatabaseObjectClass() {
		return DBOTableTransaction.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return Lists.newArrayList(new DBOTransactionToVersion());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result + ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
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
		DBOTableTransaction other = (DBOTableTransaction) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (startedBy == null) {
			if (other.startedBy != null)
				return false;
		} else if (!startedBy.equals(other.startedBy))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		if (transactionId == null) {
			if (other.transactionId != null)
				return false;
		} else if (!transactionId.equals(other.transactionId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOTableTransaction [transactionId=" + transactionId + ", tableId=" + tableId + ", startedBy="
				+ startedBy + ", startedOn=" + startedOn + ", etag=" + etag + "]";
	}

}
