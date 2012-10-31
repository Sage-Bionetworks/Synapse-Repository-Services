package org.sagebionetworks.competition.dao;

import java.util.List;

import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface ParticipantDAO {

	public void create(Participant dto) throws DatastoreException;

	public Participant get(String userId, String compId)
			throws DatastoreException, NotFoundException;

	public List<Participant> getInRange(long startIncl, long endExcl)
			throws DatastoreException, NotFoundException;

	List<Participant> getAllByCompetition(String compId)
	throws DatastoreException, NotFoundException;

	public long getCount() throws DatastoreException, NotFoundException;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String userId, String compId) throws DatastoreException,
			NotFoundException;

}