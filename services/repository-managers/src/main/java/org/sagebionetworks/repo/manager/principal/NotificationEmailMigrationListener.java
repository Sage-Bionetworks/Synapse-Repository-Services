package org.sagebionetworks.repo.manager.principal;

import java.util.List;

import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.principal.DBOPrincipalAlias;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationEmailMigrationListener implements
		MigrationTypeListener {
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		if (!type.equals(MigrationType.PRINCIPAL_ALIAS)) return;
		for (D dbo : delta) {
			if (!(dbo instanceof DBOPrincipalAlias)) continue;
			DBOPrincipalAlias pa = (DBOPrincipalAlias)dbo;
			if (!pa.getAliasType().equals(AliasEnum.USER_EMAIL)) continue;
			try {
				notificationEmailDao.getNotificationEmailForPrincipal(pa.getPrincipalId());
			} catch (NotFoundException e) {
				PrincipalAlias dto = new PrincipalAlias();
				dto.setAlias(pa.getAliasDisplay());
				dto.setAliasId(pa.getId());
				dto.setPrincipalId(pa.getPrincipalId());
				dto.setType(AliasType.USER_EMAIL);
				notificationEmailDao.create(dto);
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// nothing to do
	}

}
