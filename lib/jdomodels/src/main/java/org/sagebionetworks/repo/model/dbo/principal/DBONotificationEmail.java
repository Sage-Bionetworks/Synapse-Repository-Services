package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
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
@Table(name = TABLE_NOTIFICATION_EMAIL, constraints = { 
		"unique key UNIQUE_NOTIFICATION_EMAIL_PRINCIPAL_ID ("+ COL_NOTIFICATION_EMAIL_PRINCIPAL_ID + ")",
		"unique key UNIQUE_NOTIFICATION_EMAIL_ALIAS_ID ("+ COL_NOTIFICATION_EMAIL_ALIAS_ID + ")" 
		})
public class DBONotificationEmail implements MigratableDatabaseObject<DBONotificationEmail, DBONotificationEmail> {
	private static TableMapping<DBONotificationEmail> tableMapping = AutoTableMapping.create(DBONotificationEmail.class);
	/**
	 * The primary key for this table.
	 */
	@Field(name = COL_NOTIFICATION_EMAIL_ID, nullable = false, primary=true, backupId=true)
	private Long id;
	
	@Field(name = COL_NOTIFICATION_EMAIL_ETAG, etag = true, varchar = 500, nullable = false)
	private String etag;
	
	/**
	 * The principal ID.
	 */
	@Field(name = COL_NOTIFICATION_EMAIL_PRINCIPAL_ID, nullable = false)
	@ForeignKey(table=TABLE_USER_GROUP, field=COL_USER_GROUP_ID ,cascadeDelete=true)
	private Long principalId;
	
	/**
	 * The principal alias ID.
	 */
	@Field(name = COL_NOTIFICATION_EMAIL_ALIAS_ID, nullable = false)
	@ForeignKey(table=TABLE_PRINCIPAL_ALIAS, field=COL_PRINCIPAL_ALIAS_ID ,cascadeDelete=true)
	private Long aliasId;
	
	@Override
	public TableMapping<DBONotificationEmail> getTableMapping() {
		return tableMapping;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasId == null) ? 0 : aliasId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
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
		DBONotificationEmail other = (DBONotificationEmail) obj;
		if (aliasId == null) {
			if (other.aliasId != null)
				return false;
		} else if (!aliasId.equals(other.aliasId))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		return true;
	}

	

}
