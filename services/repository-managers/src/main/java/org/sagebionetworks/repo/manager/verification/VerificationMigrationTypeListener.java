package org.sagebionetworks.repo.manager.verification;

import java.util.List;

import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAOImpl;
import org.sagebionetworks.repo.model.dbo.verification.VerificationSubmissionHelper;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationMigrationTypeListener implements MigrationTypeListener {
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type!=MigrationType.VERIFICATION_SUBMISSION) return;
		for (D record : delta) {
			DBOVerificationSubmission dbo = (DBOVerificationSubmission) record;
			VerificationSubmission dto = VerificationSubmissionHelper.deserializeDTO(dbo);
			if (dto.getNotificationEmail()!=null) {
				// no backfill required
				continue;
			}
			
			// if there is just one captured email then it must have been the notification email
			if(dto.getEmails().size()==1) {
				dto.setNotificationEmail(dto.getEmails().get(0));
			} else {
				// most of the remaining can be disambiguated by the current notification email:
				//
				// Note: org.sagebionetworks.repo.model.migration.MigrationType shows that 
				// VERIFICATION_SUBMISSION migrates *after* NOTIFICATION_EMAIL
				String currentNotificationEmail = notificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(dto.getCreatedBy()));
				if (dto.getEmails().contains(currentNotificationEmail)) {
					dto.setNotificationEmail(currentNotificationEmail);
				} else {
					// For a tiny number we'll just use the first of the list
					dto.setNotificationEmail(dto.getEmails().get(0));
				}
			}
			
			dbo.setSerialized(VerificationSubmissionHelper.serializeDTO(dto));
			
			basicDao.update(dbo);
			
		}

	}

}
