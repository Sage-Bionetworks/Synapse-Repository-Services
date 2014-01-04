package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Listens to migration changes of principals.
 * 
 * @author jmhill
 *
 */
public class PrincipalMigrationListenerImpl implements MigrationTypeListener {
	
	@Autowired
	private PrincipalAliasDAO principalAliasDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		// We only care about principals
		if(MigrationType.PRINCIPAL == type){
			for(D dbo: delta){
				if(dbo instanceof DBOUserGroup){
					DBOUserGroup dboUserGroup = (DBOUserGroup) dbo;
					// Transform from the dbo into the new binding
					PrincipalAlias newAlias = AliasUtils.transformOldUserGroup(dboUserGroup);
					// Bind the data
					try {
						principalAliasDao.bindAliasToPrincipal(newAlias);
					} catch (NotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// Nothing to do here as the cascade delete will take care of deletes
	}

}
