package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.timeseries.TimeSeries;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesCollection;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ParticipantDataManager {

	List<ParticipantDataRow> appendData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException;

	List<ParticipantDataRow> appendData(UserInfo userInfo, ParticipantDataId participantId, String participantDataId,
			List<ParticipantDataRow> data) throws DatastoreException, NotFoundException, IOException;

	List<ParticipantDataRow> updateData(UserInfo userInfo, String participantDataId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException;

	void deleteRows(UserInfo userInfo, String participantDataId, IdList rowIds) throws IOException, NotFoundException,
			GeneralSecurityException;

	PaginatedResults<ParticipantDataRow> getData(UserInfo userInfo, String participantDataId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException;

	ParticipantDataCurrentRow getCurrentData(UserInfo userInfo, String participantDataId) throws DatastoreException, NotFoundException,
			IOException, GeneralSecurityException;

	ParticipantDataRow getDataRow(UserInfo userInfo, String participantDataId, Long rowId) throws DatastoreException, NotFoundException,
			IOException, GeneralSecurityException;

	TimeSeriesCollection getTimeSeries(UserInfo userInfo, String participantDataId, List<String> columnNames) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException;

	TimeSeriesCollection getTimeSeries(UserInfo userInfo, String participantDataId, String columnName, String alignBy)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException;
}
