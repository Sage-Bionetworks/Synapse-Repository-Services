package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_IDP_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REALM_IDP_REALM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_REALM_IDP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REALM_IDP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Mapping between groups and nodes. Used to relate Teams to Challenges
 */
public class DBORealmIdentityProvider implements MigratableDatabaseObject<DBORealmIdentityProvider, DBORealmIdentityProvider> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("realmId", COL_REALM_IDP_REALM_ID, true).withIsBackupId(true),
			new FieldColumn("identityProvider", COL_REALM_IDP_PROVIDER, true)
	};

	private Long realmId;
	private String identityProvider;

	@Override
	public TableMapping<DBORealmIdentityProvider> getTableMapping() {
		return new TableMapping<DBORealmIdentityProvider>() {
			
			@Override
			public DBORealmIdentityProvider mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBORealmIdentityProvider dbo = new DBORealmIdentityProvider();
				dbo.setRealmId(rs.getLong(COL_REALM_IDP_REALM_ID));
				dbo.setIdentityProvider(rs.getString(COL_REALM_IDP_PROVIDER));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_REALM_IDP;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_FILE_REALM_IDP;
			}
			
			@Override
			public Class<? extends DBORealmIdentityProvider> getDBOClass() {
				return DBORealmIdentityProvider.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.REALM_IDP;
	}

	@Override
	public MigratableTableTranslation<DBORealmIdentityProvider, DBORealmIdentityProvider> getTranslator() {
		// We do not currently have a backup for this object.
		return new BasicMigratableTableTranslation<DBORealmIdentityProvider>();
	}

	@Override
	public Class<? extends DBORealmIdentityProvider> getBackupClass() {
		return DBORealmIdentityProvider.class;
	}

	@Override
	public Class<? extends DBORealmIdentityProvider> getDatabaseObjectClass() {
		return DBORealmIdentityProvider.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getRealmId() {
		return realmId;
	}

	public void setRealmId(Long realmId) {
		this.realmId = realmId;
	}

	public String getIdentityProvider() {
		return identityProvider;
	}

	public void setIdentityProvider(String identityProvider) {
		this.identityProvider = identityProvider;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identityProvider == null) ? 0 : identityProvider.hashCode());
		result = prime * result + ((realmId == null) ? 0 : realmId.hashCode());
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
		DBORealmIdentityProvider other = (DBORealmIdentityProvider) obj;
		if (identityProvider == null) {
			if (other.identityProvider != null)
				return false;
		} else if (!identityProvider.equals(other.identityProvider))
			return false;
		if (realmId == null) {
			if (other.realmId != null)
				return false;
		} else if (!realmId.equals(other.realmId))
			return false;
		return true;
	}

}
