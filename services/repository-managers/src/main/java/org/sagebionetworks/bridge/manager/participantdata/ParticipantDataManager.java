package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataManager {

	RowSet appendData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	RowSet appendData(UserInfo userInfo, String participantId, String participantDataId, RowSet data) throws DatastoreException,
			NotFoundException, IOException;

	RowSet updateData(UserInfo userInfo, String participantDataId, RowSet data) throws DatastoreException, NotFoundException, IOException;

	RowSet getData(UserInfo userInfo, String participantDataId) throws DatastoreException, NotFoundException, IOException;
}
