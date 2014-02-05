package org.sagebionetworks.bridge.model;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataDAO {

	List<ParticipantDataRow> append(ParticipantDataId participantDataId, String participantDataDescriptorId, List<ParticipantDataRow> data,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException;

	List<ParticipantDataRow> update(ParticipantDataId participantDataId, String participantDataDescriptorId, List<ParticipantDataRow> data,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException;

	void deleteRows(ParticipantDataId participantDataId, String participantDataDescriptorId, IdList rowIds) throws IOException,
			NotFoundException;

	List<ParticipantDataRow> get(ParticipantDataId participantDataId, String participantDataDescriptorId,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException;

	ParticipantDataRow getRow(ParticipantDataId participantDataId, String participantDataDescriptorId, Long rowId,
			List<ParticipantDataColumnDescriptor> columns) throws DatastoreException, NotFoundException, IOException;

	void delete(ParticipantDataId participantDataId, String participantDataDescriptorId) throws DatastoreException, NotFoundException,
			IOException;

	ParticipantDataId findParticipantForParticipantData(List<ParticipantDataId> participantIds, String participantDataId);
}
