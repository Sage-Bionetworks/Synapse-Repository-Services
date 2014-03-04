package org.sagebionetworks.bridge.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.sagebionetworks.bridge.model.timeseries.TimeSeriesTable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface TimeSeriesService {
	public TimeSeriesTable getTimeSeries(Long userId, String participantDataId, List<String> columnNames) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException;
}
