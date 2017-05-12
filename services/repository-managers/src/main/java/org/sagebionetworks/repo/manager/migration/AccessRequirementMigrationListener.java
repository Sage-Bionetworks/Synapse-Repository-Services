package org.sagebionetworks.repo.manager.migration;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirement;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessRequirementRevision;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This listener is used to populate AccessRequirementRevision from existing AccessRequirement
 */
public class AccessRequirementMigrationListener implements MigrationTypeListener{

	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type != MigrationType.ACCESS_REQUIREMENT) {
			return;
		}
		for (D dbo : delta) {
			DBOAccessRequirement dboAR = (DBOAccessRequirement) dbo;
			DBOAccessRequirementRevision dboARR = AccessRequirementUtils.copyDBOAccessRequirementToDBOAccessRequirementRevision(dboAR);
			basicDao.createNew(dboARR);
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// NA
	}

}
