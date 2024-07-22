package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;

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
 * This table tracks which of a users email addresses is used for notification
 * 
 * @author brucehoff
 *
 */
public class DBONotificationEmail implements MigratableDatabaseObject<DBONotificationEmail, DBONotificationEmail> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_NOTIFICATION_EMAIL_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_NOTIFICATION_EMAIL_ETAG).withIsEtag(true),
			new FieldColumn("principalId", COL_NOTIFICATION_EMAIL_PRINCIPAL_ID),
			new FieldColumn("aliasId", COL_NOTIFICATION_EMAIL_ALIAS_ID),
	};

	/**
	 * The primary key for this table.
	 */
	private Long id;
	private String etag;
	/**
	 * The principal ID.
	 */
	private Long principalId;
	/**
	 * The principal alias ID.
	 */
	private Long aliasId;
	
	@Override
	public TableMapping<DBONotificationEmail> getTableMapping() {
		return new TableMapping<DBONotificationEmail>() {
			
			@Override
			public DBONotificationEmail mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBONotificationEmail dbo = new DBONotificationEmail();
				dbo.setId(rs.getLong(COL_NOTIFICATION_EMAIL_ID));
				dbo.setEtag(rs.getString(COL_NOTIFICATION_EMAIL_ETAG));
				dbo.setPrincipalId(rs.getLong(COL_NOTIFICATION_EMAIL_PRINCIPAL_ID));
				dbo.setAliasId(rs.getLong(COL_NOTIFICATION_EMAIL_ALIAS_ID));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_NOTIFICATION_EMAIL;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_NOTIFICATION_EMAIL;
			}
			
			@Override
			public Class<? extends DBONotificationEmail> getDBOClass() {
				return DBONotificationEmail.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NOTIFICATION_EMAIL;
	}

	@Override
	public MigratableTableTranslation<DBONotificationEmail, DBONotificationEmail> getTranslator() {
		return new BasicMigratableTableTranslation<DBONotificationEmail>();
	}

	@Override
	public Class<? extends DBONotificationEmail> getBackupClass() {
		return DBONotificationEmail.class;
	}

	@Override
	public Class<? extends DBONotificationEmail> getDatabaseObjectClass() {
		return DBONotificationEmail.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

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

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Long getAliasId() {
		return aliasId;
	}

	public void setAliasId(Long aliasId) {
		this.aliasId = aliasId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliasId, etag, id, principalId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBONotificationEmail other = (DBONotificationEmail) obj;
		return Objects.equals(aliasId, other.aliasId) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(principalId, other.principalId);
	}

	@Override
	public String toString() {
		return "DBONotificationEmail [id=" + id + ", etag=" + etag + ", principalId=" + principalId + ", aliasId="
				+ aliasId + "]";
	}
	
}
