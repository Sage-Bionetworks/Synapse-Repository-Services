package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.repo.manager.table.metadata.ObjectDataProvider;
import org.sagebionetworks.repo.model.IdAndChecksum;
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

	@Autowired
	public SubmissionObjectProvider(SubmissionDAO submissionDao) {
		super();
		this.submissionDao = submissionDao;
	}

	@Override
	public Iterator<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return submissionDao.getSubmissionData(objectIds, maxAnnotationChars).iterator();
	}

	@Override
	public ReplicationType getReplicationType() {
		return ReplicationType.SUBMISSION;
	}

	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForChildren(Long salt, Set<Long> parentIds,
			Set<SubType> subTypes) {
		return new PaginationIterator<IdAndChecksum>((long limit, long offset) -> {
			return submissionDao.getIdAndChecksumsPage(salt, parentIds, limit, offset);
		}, PAGE_SIZE);
	}

	@Override
	public Iterator<IdAndChecksum> streamOverIdsAndChecksumsForObjects(Long salt, Set<Long> objectIds) {
		throw new UnsupportedOperationException("All submission views are hierarchical");
	}

}
