package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_RECEIPT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATION_RECEIPT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AUTHENTICATION_RECEIPT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATION_RECEIPT_EXPIRATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAuthenticationReceipt implements MigratableDatabaseObject<DBOAuthenticationReceipt, DBOAuthenticationReceipt>{

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_AUTHENTICATION_RECEIPT_ID, true).withIsBackupId(true),
		new FieldColumn("userId", COL_AUTHENTICATION_RECEIPT_USER_ID),
		new FieldColumn("receipt", COL_AUTHENTICATION_RECEIPT_RECEIPT).withIsEtag(true),
		new FieldColumn("expiration", COL_AUTHENTICATION_RECEIPT_EXPIRATION)
	};

	private Long id;
	private Long userId;
	private String receipt;
	private Long expiration;

	@Override
	public String toString() {
		return "DBOAuthenticationReceipt [id=" + id + ", userId=" + userId + ", receipt=" + receipt + ", createdOn="
				+ expiration + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((receipt == null) ? 0 : receipt.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		DBOAuthenticationReceipt other = (DBOAuthenticationReceipt) obj;
		if (expiration == null) {
			if (other.expiration != null)
				return false;
		} else if (!expiration.equals(other.expiration))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (receipt == null) {
			if (other.receipt != null)
				return false;
		} else if (!receipt.equals(other.receipt))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getReceipt() {
		return receipt;
	}

	public void setReceipt(String receipt) {
		this.receipt = receipt;
	}

	public Long getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration;
	}

	@Override
	public TableMapping<DBOAuthenticationReceipt> getTableMapping() {
		return new TableMapping<DBOAuthenticationReceipt>(){

			@Override
			public DBOAuthenticationReceipt mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAuthenticationReceipt dbo = new DBOAuthenticationReceipt();
				dbo.setId(rs.getLong(COL_AUTHENTICATION_RECEIPT_ID));
				dbo.setUserId(rs.getLong(COL_AUTHENTICATION_RECEIPT_USER_ID));
				dbo.setReceipt(rs.getString(COL_AUTHENTICATION_RECEIPT_RECEIPT));
				dbo.setExpiration(rs.getLong(COL_AUTHENTICATION_RECEIPT_EXPIRATION));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_AUTHENTICATION_RECEIPT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AUTHENTICATION_RECEIPT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAuthenticationReceipt> getDBOClass() {
				return DBOAuthenticationReceipt.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AUTHENTICATION_RECEIPT;
	}

	@Override
	public MigratableTableTranslation<DBOAuthenticationReceipt, DBOAuthenticationReceipt> getTranslator() {
		return new BasicMigratableTableTranslation<DBOAuthenticationReceipt>();
	}

	@Override
	public Class<? extends DBOAuthenticationReceipt> getBackupClass() {
		return DBOAuthenticationReceipt.class;
	}

	@Override
	public Class<? extends DBOAuthenticationReceipt> getDatabaseObjectClass() {
		return DBOAuthenticationReceipt.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
