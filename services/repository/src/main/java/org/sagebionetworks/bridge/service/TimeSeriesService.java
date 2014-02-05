package org.sagebionetworks.bridge.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.timeseries.TimeSeries;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesCollection;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface TimeSeriesService {
	public TimeSeriesCollection getTimeSeries(Long userId, String participantDataId, List<String> columnNames) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException;

	public TimeSeriesCollection getTimeSeries(Long userId, String participantDataId, String columnName, String alignBy)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException;
}
