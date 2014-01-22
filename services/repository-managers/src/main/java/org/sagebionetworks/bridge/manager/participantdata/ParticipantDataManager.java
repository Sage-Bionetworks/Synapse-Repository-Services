package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataManager {

	List<ParticipantDataRow> appendData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException;

	List<ParticipantDataRow> appendData(UserInfo userInfo, String participantId, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException;

	List<ParticipantDataRow> updateData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException;

	PaginatedResults<ParticipantDataRow> getData(UserInfo userInfo, String participantDataId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException;

	ParticipantDataCurrentRow getCurrentData(UserInfo userInfo, String participantDataId)
			throws DatastoreException, NotFoundException, IOException;

	ParticipantDataRow getDataRow(UserInfo userInfo, String participantDataId, Long rowId) throws DatastoreException,
			NotFoundException, IOException;
}
