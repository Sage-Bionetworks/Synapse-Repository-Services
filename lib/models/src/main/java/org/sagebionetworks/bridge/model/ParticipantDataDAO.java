package org.sagebionetworks.bridge.model;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataDAO {

	RowSet append(String participantId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException,
			IOException;

	RowSet update(String participantId, String participantDataId, RowSet data) throws DatastoreException, NotFoundException,
			IOException;

	RowSet get(String participantId, String participantDataId) throws DatastoreException, NotFoundException, IOException;

	void delete(String participantId, String participantDataId) throws DatastoreException, NotFoundException, IOException;

	String findParticipantForParticipantData(List<String> participantIds, String participantDataId);
}
