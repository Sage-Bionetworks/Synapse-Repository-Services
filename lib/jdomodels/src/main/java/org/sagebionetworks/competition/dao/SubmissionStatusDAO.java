package org.sagebionetworks.competition.dao;

import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.web.NotFoundException;

public interface SubmissionStatusDAO extends MigratableDAO {

	public SubmissionStatus create(SubmissionStatus dto)
			throws DatastoreException;

	public SubmissionStatus get(String id) throws DatastoreException,
			NotFoundException;

	public void update(SubmissionStatus dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException;

	public void updateFromBackup(SubmissionStatus dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException;

	public void delete(String id) throws DatastoreException, NotFoundException;

}