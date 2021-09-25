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
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.util.PaginationIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionObjectProvider implements ObjectDataProvider {

	public static final int PAGE_SIZE = 10_000;

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
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForChildren(Long salt, Set<Long> parentIds,
			Set<SubType> subTypes) {
		return new PaginationIterator<IdAndChecksum>((long limit, long offset) -> {
			return submissionDao.getIdAndChecksumsPage(salt, parentIds, subTypes, limit, offset);
		}, PAGE_SIZE);
	}

	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForObjects(Long salt, Set<Long> objectIds) {
		throw new UnsupportedOperationException("All submission views are hierarchical");
	}

}
