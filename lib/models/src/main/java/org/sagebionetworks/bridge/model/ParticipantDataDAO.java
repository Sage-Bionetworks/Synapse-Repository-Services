package org.sagebionetworks.bridge.model;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataDAO {

	List<ParticipantDataRow> append(String participantId, String participantDataId, List<ParticipantDataRow> data,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException,
			NotFoundException, IOException;

	List<ParticipantDataRow> update(String participantId, String participantDataId, List<ParticipantDataRow> data,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException,
			NotFoundException, IOException;

	List<ParticipantDataRow> get(String participantId, String participantDataId, List<ParticipantDataColumnDescriptor> columns)
			throws DatastoreException, NotFoundException, IOException;

	ParticipantDataRow getRow(String participantId, String participantDataId, Long rowId, List<ParticipantDataColumnDescriptor> columns)
			throws DatastoreException,
			NotFoundException, IOException;

	void delete(String participantId, String participantDataId) throws DatastoreException, NotFoundException, IOException;

	String findParticipantForParticipantData(List<String> participantIds, String participantDataId);
}
