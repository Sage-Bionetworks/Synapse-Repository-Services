package org.sagebionetworks.bridge.service;

import java.io.IOException;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.table.PaginatedRowSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataService {
	public PaginatedRowSet get(String userId, String participantDataId, Integer limit, Integer offset) throws DatastoreException,
			NotFoundException, IOException;

	public RowSet append(String userId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	public RowSet append(String userId, String participantId, String participantDataId, RowSet data) throws DatastoreException,
			NotFoundException,
			IOException;

	public RowSet update(String userId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	public ParticipantDataDescriptor createParticipantDataDescriptor(String userId, ParticipantDataDescriptor participantDataDescriptor) throws DatastoreException,
			NotFoundException;

	public ParticipantDataDescriptor getParticipantDataDescriptor(String userId, String participantDataId) throws DatastoreException, NotFoundException;

	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(String userId, Integer limit, Integer offset) throws DatastoreException,
			NotFoundException;

	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(String userId, Integer limit, Integer offset) throws DatastoreException,
			NotFoundException;

	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(String userId, ParticipantDataColumnDescriptor participantDataColumnDescriptor)
			throws DatastoreException, NotFoundException;

	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptors(String userId, String participantDataId, Integer limit,
			Integer offset)
			throws DatastoreException, NotFoundException;
}
