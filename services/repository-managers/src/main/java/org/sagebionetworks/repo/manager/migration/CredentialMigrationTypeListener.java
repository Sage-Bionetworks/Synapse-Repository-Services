package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

public class CredentialMigrationTypeListener implements MigrationTypeListener {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type.equals(MigrationType.CREDENTIAL)) {
			for (D d : delta) {
				if (d instanceof DBOCredential) {
					DBOCredential credential = (DBOCredential)d;
					
					DBOSessionToken sessionToken = new DBOSessionToken();
					sessionToken.setPrincipalId(credential.getPrincipalId());
					
					sessionToken.setDomain(DomainType.SYNAPSE);
					sessionToken.setSessionToken(credential.getSessionToken());
					sessionToken.setValidatedOn(credential.getValidatedOn());
					basicDao.createOrUpdate(sessionToken);
					
					DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
					tou.setPrincipalId(credential.getPrincipalId());
					tou.setAgreesToTermsOfUse(credential.getAgreesToTermsOfUse());
					tou.setDomain(DomainType.SYNAPSE);
					basicDao.createOrUpdate(tou);
				}
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// delete of user cascades.
	}

}
