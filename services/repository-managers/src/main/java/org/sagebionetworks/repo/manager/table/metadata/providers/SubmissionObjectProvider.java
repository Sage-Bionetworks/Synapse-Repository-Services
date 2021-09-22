package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
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
	public Iterator<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return submissionDao.getSubmissionData(objectIds, maxAnnotationChars).iterator();
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

	@Override
	public Iterator<IdAndChecksum> streamOverViewIds(long checksumSalt, ViewFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long calculateViewChecksum(long checksumSalt, ViewFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
