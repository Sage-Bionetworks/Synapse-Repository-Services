package org.sagebionetworks.bridge.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataDescriptionManager;
import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataManager;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.timeseries.TimeSeries;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesCollection;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TimeSeriesServiceImpl implements TimeSeriesService {
	@Autowired
	private UserManager userManager;
	@Autowired
	private ParticipantDataManager participantDataManager;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Override
	public TimeSeriesCollection getTimeSeries(Long userId, String participantDataId, List<String> columnNames) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getTimeSeries(userInfo, participantDataId, columnNames);
	}

	@Override
	public TimeSeriesCollection getTimeSeries(Long userId, String participantDataId, String columnName, String alignBy)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return participantDataManager.getTimeSeries(userInfo, participantDataId, columnName, alignBy);
	}
}
