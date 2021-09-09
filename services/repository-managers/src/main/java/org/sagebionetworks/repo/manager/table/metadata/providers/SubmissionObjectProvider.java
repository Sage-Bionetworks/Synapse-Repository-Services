package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionObjectProvider implements ObjectDataProvider {
	
	private final SubmissionDAO submissionDao;
	private final EvaluationDAO evaluationDao;
	
	@Autowired
	public SubmissionObjectProvider(SubmissionDAO submissionDao, EvaluationDAO evaluationDao) {
		super();
		this.submissionDao = submissionDao;
		this.evaluationDao = evaluationDao;
	}

	@Override
	public List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return submissionDao.getSubmissionData(objectIds, maxAnnotationChars);
	}

	@Override
	public Set<Long> getAvailableContainers(List<Long> containerIds) {
		return evaluationDao.getAvailableEvaluations(containerIds);
	}

	@Override
	public List<IdAndEtag> getChildren(Long containerId) {
		return submissionDao.getSubmissionIdAndEtag(containerId);
	}

	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds) {
		return submissionDao.getSumOfSubmissionCRCsForEachEvaluation(containerIds);
	}

	@Override
	public ReplicationType getReplicationType() {
		return ReplicationType.SUBMISSION;
	}
	
}
