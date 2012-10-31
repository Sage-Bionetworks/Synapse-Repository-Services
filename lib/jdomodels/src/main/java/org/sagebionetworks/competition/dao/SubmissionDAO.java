package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface SubmissionDAO {

	public void create(Submission dto) throws DatastoreException;

	public Submission get(String id) throws DatastoreException,
			NotFoundException;

	public List<Submission> getAllByUser(String userId)
			throws DatastoreException, NotFoundException;

	public List<Submission> getAllByCompetition(String compId)
			throws DatastoreException, NotFoundException;

	public List<Submission> getAllByCompetitionAndStatus(String compId,
			SubmissionStatus status) throws DatastoreException,
			NotFoundException;

	public long getCount() throws DatastoreException, NotFoundException;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException;

}