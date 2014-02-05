package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDatetimeValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.data.value.ValueTranslator;
import org.sagebionetworks.bridge.model.timeseries.TimeSeries;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesCollection;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesPoint;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ParticipantDataManagerImpl implements ParticipantDataManager {

	private final static ParticipantDataRow EMPTY_ROW;

	static {
		EMPTY_ROW = new ParticipantDataRow();
		EMPTY_ROW.setData(Collections.<String, ParticipantDataValue> emptyMap());
	}

	@Autowired
	private ParticipantDataDAO participantDataDAO;
	@Autowired
	private ParticipantDataIdMappingManager participantDataMappingManager;
	@Autowired
	private ParticipantDataStatusDAO participantDataStatusDAO;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Override
	public List<ParticipantDataRow> appendData(UserInfo userInfo, ParticipantDataId participantDataId, String participantDataDescriptorId,
			List<ParticipantDataRow> data) throws DatastoreException, NotFoundException, IOException {
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		return participantDataDAO.append(participantDataId, participantDataDescriptorId, data, columns);
	}

	@Override
	public List<ParticipantDataRow> appendData(UserInfo userInfo, String participantDataDescriptorId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			participantDataId = participantDataMappingManager.createNewParticipantIdForUser(userInfo);
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		return participantDataDAO.append(participantDataId, participantDataDescriptorId, data, columns);
	}

	@Override
	public void deleteRows(UserInfo userInfo, String participantDataDescriptorId, IdList rowIds) throws IOException, NotFoundException,
			GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		participantDataDAO.deleteRows(participantDataId, participantDataDescriptorId, rowIds);
	}

	@Override
	public List<ParticipantDataRow> updateData(UserInfo userInfo, String participantDataDescriptorId, List<ParticipantDataRow> data)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			throw new NotFoundException("No data to update found for this user");
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		return participantDataDAO.update(participantDataId, participantDataDescriptorId, data, columns);
	}

	@Override
	public PaginatedResults<ParticipantDataRow> getData(UserInfo userInfo, String participantDataDescriptorId, Integer limit, Integer offset)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			return PaginatedResultsUtil.createEmptyPaginatedResults();
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		List<ParticipantDataRow> rowList = participantDataDAO.get(participantDataId, participantDataDescriptorId, columns);
		return PaginatedResultsUtil.createPaginatedResults(rowList, limit, offset);
	}

	@Override
	public ParticipantDataRow getDataRow(UserInfo userInfo, String participantDataDescriptorId, Long rowId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			throw new NotFoundException("No participant data with id " + participantDataDescriptorId);
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		return participantDataDAO.getRow(participantDataId, participantDataDescriptorId, rowId, columns);
	}

	@Override
	public ParticipantDataCurrentRow getCurrentData(UserInfo userInfo, String participantDataDescriptorId) throws DatastoreException,
			NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataCurrentRow result = new ParticipantDataCurrentRow();
		result.setDescriptor(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, participantDataDescriptorId));
		result.setColumns(participantDataDescriptionManager.getColumns(participantDataDescriptorId));
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return empty status
			ParticipantDataStatus status = new ParticipantDataStatus();
			status.setParticipantDataDescriptorId(participantDataDescriptorId);
			result.setStatus(status);
			return result;
		}

		ParticipantDataStatus status = participantDataStatusDAO.getParticipantStatus(participantDataId, result.getDescriptor());
		result.setStatus(status);

		result.setCurrentData(EMPTY_ROW);
		result.setPreviousData(EMPTY_ROW);

		List<ParticipantDataRow> rowList = participantDataDAO.get(participantDataId, participantDataDescriptorId, result.getColumns());
		ListIterator<ParticipantDataRow> iter = rowList.listIterator(rowList.size());
		if (iter.hasPrevious()) {
			ParticipantDataRow lastRow = iter.previous();
			if (BooleanUtils.isFalse(status.getLastEntryComplete())) {
				result.setCurrentData(lastRow);
				if (iter.hasPrevious()) {
					result.setPreviousData(iter.previous());
				}
			} else {
				result.setPreviousData(lastRow);
			}
		}
		return result;
	}

	private ParticipantDataId getParticipantDataId(UserInfo userInfo, String participantDataDescriptorId) throws IOException,
			GeneralSecurityException {
		List<ParticipantDataId> participantDataIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		ParticipantDataId participantDataId = participantDataDAO.findParticipantForParticipantData(participantDataIds,
				participantDataDescriptorId);
		return participantDataId;
	}

	@Override
	public TimeSeriesCollection getTimeSeries(UserInfo userInfo, String participantDataDescriptorId, List<String> columnNames)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		List<ParticipantDataId> participantDataIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		ParticipantDataId participantDataId = participantDataDAO.findParticipantForParticipantData(participantDataIds,
				participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			throw new NotFoundException("No participant data with id " + participantDataDescriptorId);
		}
		ParticipantDataDescriptor dataDescriptor = participantDataDescriptionManager.getParticipantDataDescriptor(userInfo,
				participantDataDescriptorId);
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		List<ParticipantDataRow> data = participantDataDAO.get(participantDataId, participantDataDescriptorId, columns);

		String datetimeColumnName = dataDescriptor.getDatetimeStartColumnName();
		if (StringUtils.isEmpty(datetimeColumnName)) {
			throw new IllegalArgumentException("Data descriptor does not define a date column for timeseries");
		}

		TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
		timeSeriesCollection.setSeries(Lists.<TimeSeries> newArrayListWithCapacity(columns.size()));

		Set<String> columnNameSet = columnNames == null ? null : Sets.newHashSet(columnNames);
		for (ParticipantDataColumnDescriptor column : columns) {
			if (columnNameSet == null || columnNameSet.contains(column.getName())) {
				if (!column.getName().equals(datetimeColumnName)) {
					TimeSeries timeSeries = createTimeSeries(data, datetimeColumnName, column.getName());
					if (!timeSeries.getSeries().isEmpty()) {
						timeSeriesCollection.getSeries().add(timeSeries);
					}
				}
			}
		}
		return timeSeriesCollection;
	}

	@Override
	public TimeSeriesCollection getTimeSeries(UserInfo userInfo, String participantDataDescriptorNameOrId, String columnName, String alignBy)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		return null;
	}

	private TimeSeries createTimeSeries(List<ParticipantDataRow> data, String datetimeColumnName, String columnName) {
		TimeSeries timeSeries = new TimeSeries();
		timeSeries.setName(columnName);
		timeSeries.setSeries(Lists.<TimeSeriesPoint> newArrayListWithCapacity(data.size()));
		for (ParticipantDataRow row : data) {
			ParticipantDataValue value = row.getData().get(columnName);
			if (value != null) {
				ParticipantDataValue datetimeRawValue = row.getData().get(datetimeColumnName);
				if (!(datetimeRawValue instanceof ParticipantDataDatetimeValue)) {
					throw new IllegalArgumentException("Date column for timeseries does not contain date type: "
							+ datetimeRawValue.getClass().getName());
				}
				ParticipantDataDatetimeValue datetimeValue = (ParticipantDataDatetimeValue) datetimeRawValue;
				if (datetimeValue != null) {
					TimeSeriesPoint point = new TimeSeriesPoint();
					point.setDate(datetimeValue.getValue());
					point.setValue(ValueTranslator.toDouble(value));
					timeSeries.getSeries().add(point);
				}
			}
		}
		return timeSeries;
	}
}
