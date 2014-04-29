package org.sagebionetworks.evaluation.manager;

import java.util.List;

import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class SubmissionMigrationListener implements MigrationTypeListener {
	
	@Autowired
	EvaluationSubmissionsDAO evaluationSubmissionsDAO;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		if (!type.equals(MigrationType.SUBMISSION_STATUS)) return;
		// For each Evaluation object, create an EvaluationSubmissions object
		for (D dbo : delta) {
			if (!(dbo instanceof SubmissionDBO)) continue;
			SubmissionDBO subStatDBO = (SubmissionDBO)dbo;
			Long evaluationId = subStatDBO.getEvalId();
			try {
				evaluationSubmissionsDAO.getForEvaluation(evaluationId);
				// OK, nothing to create
			} catch (NotFoundException e) {
				evaluationSubmissionsDAO.createForEvaluation(evaluationId);
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// nothing to do 
	}

}
