package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAuthorizationConsent implements MigratableDatabaseObject<DBOAuthorizationConsent, DBOAuthorizationConsent> {

	@Override
	public TableMapping<DBOAuthorizationConsent> getTableMapping() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MigrationType getMigratableTableType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MigratableTableTranslation<DBOAuthorizationConsent, DBOAuthorizationConsent> getTranslator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends DBOAuthorizationConsent> getBackupClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends DBOAuthorizationConsent> getDatabaseObjectClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		// TODO Auto-generated method stub
		return null;
	}

}
