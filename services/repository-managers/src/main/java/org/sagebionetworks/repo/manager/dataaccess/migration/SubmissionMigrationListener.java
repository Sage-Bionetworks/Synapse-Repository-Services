package org.sagebionetworks.repo.manager.dataaccess.migration;

import java.util.List;

import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DBOSubmission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "Migration to normalize accessor changes for submission (See PLFM-6917)")
public class SubmissionMigrationListener implements MigrationTypeListener<DBOSubmission> {
	
	private SubmissionDAO submissionDao;

	@Autowired
	public SubmissionMigrationListener(SubmissionDAO submissionDao) { 
		this.submissionDao = submissionDao;
	}

	@Override
	public boolean supports(MigrationType type) {
		return MigrationType.DATA_ACCESS_SUBMISSION == type;
	}

	@Override
	public void beforeCreateOrUpdate(List<DBOSubmission> batch) {
	}

	@Override
	public void afterCreateOrUpdate(List<DBOSubmission> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}
		
		batch.forEach(dbo -> {
			Submission submission = SubmissionUtils.readSerializedField(dbo.getSubmissionSerialized());
			
			submission.setId(dbo.getId().toString());
			
			submissionDao.backFillAccessorChangesIfNeeded(submission);
		});
	}

}
