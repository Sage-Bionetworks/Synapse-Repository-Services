package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.units.UnitConversionUtils;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDatetimeValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataEventValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLabValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.data.value.ValueFactory;
import org.sagebionetworks.bridge.model.data.value.ValueTranslator;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesColumn;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesRow;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesTable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class ParticipantDataManagerImpl implements ParticipantDataManager {

	private final static ParticipantDataRow EMPTY_ROW;

	static {
		EMPTY_ROW = new ParticipantDataRow();
		EMPTY_ROW.setData(Collections.<String, ParticipantDataValue> emptyMap());
	}

	private static class ColumnComparator implements Comparator<ParticipantDataRow> {
		private final String columnName;

		public ColumnComparator(String columnName) {
			this.columnName = columnName;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compare(ParticipantDataRow o1, ParticipantDataRow o2) {
			return ValueTranslator.getComparable(o1.getData().get(columnName)).compareTo(
					ValueTranslator.getComparable(o2.getData().get(columnName)));
		}
	}

	private static abstract class EventComparator implements Comparator<ParticipantDataRow> {
		private final String eventColumnName;

		public EventComparator(String eventColumnName) {
			this.eventColumnName = eventColumnName;
		}

		@Override
		public int compare(ParticipantDataRow o1, ParticipantDataRow o2) {
			ParticipantDataEventValue eventValue1 = (ParticipantDataEventValue) o1.getData().get(eventColumnName);
			ParticipantDataEventValue eventValue2 = (ParticipantDataEventValue) o2.getData().get(eventColumnName);
			return compare(eventValue1,eventValue2);
		}

		abstract public int compare(ParticipantDataEventValue e1, ParticipantDataEventValue e2);
	}

	@Autowired
	private ParticipantDataDAO participantDataDAO;
	@Autowired
	private ParticipantDataIdMappingManager participantDataMappingManager;
	@Autowired
	private ParticipantDataStatusDAO participantDataStatusDAO;
	@Autowired
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	public ParticipantDataManagerImpl() {
	}

	// for unit testing only
	ParticipantDataManagerImpl(ParticipantDataDAO participantDataDAO, ParticipantDataIdMappingManager participantDataMappingManager,
			ParticipantDataStatusDAO participantDataStatusDAO, ParticipantDataDescriptionManager participantDataDescriptionManager) {
		this.participantDataDAO = participantDataDAO;
		this.participantDataMappingManager = participantDataMappingManager;
		this.participantDataStatusDAO = participantDataStatusDAO;
		this.participantDataDescriptionManager = participantDataDescriptionManager;
	}

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
	public PaginatedResults<ParticipantDataRow> getData(UserInfo userInfo, String participantDataDescriptorId,
			Integer limit, Integer offset, boolean normalizeData) throws DatastoreException, NotFoundException,
			IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result. It will have no headers given the way this works.
			return PaginatedResultsUtil.createEmptyPaginatedResults();
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		List<ParticipantDataRow> rowList = participantDataDAO.get(participantDataId, participantDataDescriptorId, columns);

		return UnitConversionUtils.maybeNormalizePagedData(normalizeData,
				PaginatedResultsUtil.createPaginatedResults(rowList, limit, offset));
	}

	@Override
	public List<ParticipantDataRow> getHistoryData(UserInfo userInfo, String participantDataDescriptorId,
			final boolean onlyNotEnded, final Date after, final Date before, SortType sortType, boolean normalizeData)
			throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type, which is not an error, so return
			// empty result.
			return Collections.<ParticipantDataRow> emptyList();
		}
		final ParticipantDataDescriptor participantDataDescriptor = participantDataDescriptionManager.getParticipantDataDescriptor(userInfo,
				participantDataDescriptorId);
		if (participantDataDescriptor.getEventColumnName() == null) {
			throw new NotFoundException("Data descriptor does not define event column");
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		List<ParticipantDataRow> rowList = participantDataDAO.get(participantDataId, participantDataDescriptorId, columns);

		// first filter out non-ended, and before and after if they apply
		rowList = Lists.newArrayList(Iterables.filter(rowList, new Predicate<ParticipantDataRow>() {
			@Override
			public boolean apply(ParticipantDataRow row) {
				ParticipantDataEventValue event = (ParticipantDataEventValue) row.getData().get(
						participantDataDescriptor.getEventColumnName());
				if (onlyNotEnded && event.getEnd() != null) {
					return false;
				}
				if (after != null && event.getStart() < after.getTime()) {
					return false;
				}
				if (before != null && event.getStart() > before.getTime()) {
					return false;
				}
				return true;
			}
		}));

		switch (sortType) {
		case SORT_BY_DATE:
			rowList = sortRowsByDate(rowList, participantDataDescriptor.getEventColumnName());
			break;
		case SORT_BY_GROUP_AND_DATE:
			rowList = sortRowsGroupAndDate(rowList, participantDataDescriptor.getEventColumnName());
			break;
		}
		return UnitConversionUtils.maybeNormalizeDataSet(normalizeData, rowList);
	}

	@Override
	public ParticipantDataRow getDataRow(UserInfo userInfo, String participantDataDescriptorId, Long rowId,
			boolean normalizeData) throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
		ParticipantDataId participantDataId = getParticipantDataId(userInfo, participantDataDescriptorId);
		if (participantDataId == null) {
			// User has never created data for this ParticipantData type
			throw new NotFoundException("No participant data with id " + participantDataDescriptorId);
		}
		List<ParticipantDataColumnDescriptor> columns = participantDataDescriptionManager.getColumns(participantDataDescriptorId);
		
		ParticipantDataRow row = participantDataDAO.getRow(participantDataId, participantDataDescriptorId, rowId, columns);
		return UnitConversionUtils.maybeNormalizeRow(normalizeData, row);
	}

	@Override
	public ParticipantDataCurrentRow getCurrentData(UserInfo userInfo, String participantDataDescriptorId,
			boolean normalizeData) throws DatastoreException, NotFoundException, IOException, GeneralSecurityException {
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
		return UnitConversionUtils.maybeNormalizeCurrentRow(normalizeData, result);
	}

	private ParticipantDataId getParticipantDataId(UserInfo userInfo, String participantDataDescriptorId) throws IOException,
			GeneralSecurityException {
		List<ParticipantDataId> participantDataIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		ParticipantDataId participantDataId = participantDataDAO.findParticipantForParticipantData(participantDataIds,
				participantDataDescriptorId);
		return participantDataId;
	}

	@Override
	public TimeSeriesTable getTimeSeries(UserInfo userInfo, String participantDataDescriptorId,
			List<String> columnNamesRequested, boolean normalizeData) throws DatastoreException, NotFoundException,
			IOException, GeneralSecurityException {
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
		
		UnitConversionUtils.maybeNormalizeDataSet(normalizeData, data);

		TimeSeriesTable timeSeriesCollection = new TimeSeriesTable();
		timeSeriesCollection.setName(dataDescriptor.getName());

		List<TimeSeriesColumn> timeSeriesColumns;
		List<TimeSeriesRow> rows = Collections.emptyList();
		List<ParticipantDataEventValue> events = Collections.emptyList();

		Long firstDate = new Date().getTime();
		Long lastDate = null;

		if (dataDescriptor.getEventColumnName() != null) {
			events = Lists.newArrayListWithExpectedSize(data.size());
			final String eventColumnName = dataDescriptor.getEventColumnName();

			timeSeriesColumns = createColumns(columnNamesRequested, columns, eventColumnName, timeSeriesCollection);

			data = sortRowsGroupAndMerge(data, eventColumnName);

			for (ParticipantDataRow row : data) {
				events.add((ParticipantDataEventValue) row.getData().get(eventColumnName));
			}
			if (!events.isEmpty()) {
				firstDate = events.get(0).getStart();
			}
		} else if (dataDescriptor.getDatetimeStartColumnName() != null) {
			rows = Lists.newArrayListWithExpectedSize(data.size());
			String datetimeColumnName = dataDescriptor.getDatetimeStartColumnName();

			timeSeriesColumns = createColumns(columnNamesRequested, columns, datetimeColumnName, timeSeriesCollection);

			// filter out data with null date time
			for (Iterator<ParticipantDataRow> iterator = data.iterator(); iterator.hasNext();) {
				ParticipantDataRow row = (ParticipantDataRow) iterator.next();
				if (row.getData().get(datetimeColumnName) == null) {
					iterator.remove();
				}
			}

			// sort timeseries by date always
			Collections.sort(data, new ColumnComparator(datetimeColumnName));

			if (!data.isEmpty()) {
				firstDate = ValueTranslator.toLong(data.get(0).getData().get(datetimeColumnName));
			}

			// and make into time series
			for (ParticipantDataRow row : data) {
				TimeSeriesRow timeSeriesRow = new TimeSeriesRow();
				timeSeriesRow.setValues(Lists.<String> newArrayListWithCapacity(timeSeriesColumns.size()));
				for (TimeSeriesColumn timeSeriesColumn : timeSeriesColumns) {
					String value = null;
					ParticipantDataValue dataValue = row.getData().get(timeSeriesColumn.getName());
					if (dataValue != null) {
						if (dataValue instanceof ParticipantDataDatetimeValue) {
							value = ((ParticipantDataDatetimeValue) dataValue).getValue().toString();
						} else if (dataValue instanceof ParticipantDataLabValue) {
							ParticipantDataLabValue labValue = ((ParticipantDataLabValue) dataValue);
							value = labValue.getValue().toString();
							timeSeriesColumn.setExpectedMinimum(min(timeSeriesColumn.getExpectedMinimum(), labValue.getMinNormal()));
							timeSeriesColumn.setExpectedMaximum(max(timeSeriesColumn.getExpectedMaximum(), labValue.getMaxNormal()));
							timeSeriesColumn.setActualMinimum(min(timeSeriesColumn.getActualMinimum(), labValue.getValue()));
							timeSeriesColumn.setActualMaximum(max(timeSeriesColumn.getActualMaximum(), labValue.getValue()));
						} else {
							value = ValueTranslator.toDouble(dataValue).toString();
						}
					}
					timeSeriesRow.getValues().add(value);
				}
				rows.add(timeSeriesRow);
			}
		} else {
			throw new IllegalArgumentException("Data descriptor does not define a date column for timeseries");
		}

		timeSeriesCollection.setEvents(events);
		timeSeriesCollection.setRows(rows);
		timeSeriesCollection.setFirstDate(firstDate);
		timeSeriesCollection.setLastDate(lastDate);
		return timeSeriesCollection;
	}

	private List<TimeSeriesColumn> createColumns(List<String> columnNamesRequested, List<ParticipantDataColumnDescriptor> columns,
			String firstColumnName, TimeSeriesTable timeSeriesCollection) {
		List<TimeSeriesColumn> timeSeriesColumns;
		int datetimeColumnIndex;
		if (columnNamesRequested != null && columnNamesRequested.size() > 0) {
			timeSeriesColumns = Lists.newArrayListWithCapacity(columnNamesRequested.size() + 1);
			datetimeColumnIndex = 0;
			TimeSeriesColumn firstColumn = new TimeSeriesColumn();
			firstColumn.setName(firstColumnName);
			timeSeriesColumns.add(firstColumn);
			for (String column : columnNamesRequested) {
				TimeSeriesColumn newColumn = new TimeSeriesColumn();
				newColumn.setName(column);
				timeSeriesColumns.add(newColumn);
			}
		} else {
			timeSeriesColumns = Lists.newArrayListWithCapacity(columns.size());
			datetimeColumnIndex = 0;
			TimeSeriesColumn firstColumn = new TimeSeriesColumn();
			firstColumn.setName(firstColumnName);
			timeSeriesColumns.add(firstColumn);
			for (ParticipantDataColumnDescriptor column : columns) {
				if (!column.getName().equals(firstColumnName) && ValueTranslator.canBeDouble(column.getColumnType())) {
					TimeSeriesColumn newColumn = new TimeSeriesColumn();
					newColumn.setName(column.getName());
					timeSeriesColumns.add(newColumn);
				}
			}
		}

		timeSeriesCollection.setDateIndex((long) datetimeColumnIndex);
		timeSeriesCollection.setColumns(timeSeriesColumns);
		return timeSeriesColumns;
	}

	private List<ParticipantDataRow> sortRowsByDate(List<ParticipantDataRow> rowList, String eventColumnName) {
		Collections.sort(rowList, new EventComparator(eventColumnName) {
			@Override
			public int compare(ParticipantDataEventValue e1, ParticipantDataEventValue e2) {
				return e1.getStart().compareTo(e2.getStart());
			}
		});
		return rowList;
	}

	private List<ParticipantDataRow> sortRowsGroupAndDate(List<ParticipantDataRow> rowList, final String eventColumnName) {
		List<List<ParticipantDataRow>> groups = presortRowsGroupAndDate(rowList, eventColumnName);
		List<ParticipantDataRow> result = Lists.newArrayListWithCapacity(rowList.size());
		for (List<ParticipantDataRow> group : groups) {
			for (ParticipantDataRow row : group) {
				result.add(row);
			}
		}
		return result;
	}

	private List<ParticipantDataRow> sortRowsGroupAndMerge(List<ParticipantDataRow> rowList, final String eventColumnName) {
		List<List<ParticipantDataRow>> groups = presortRowsGroupAndDate(rowList, eventColumnName);
		List<ParticipantDataRow> result = Lists.newArrayListWithCapacity(groups.size());
		for (List<ParticipantDataRow> group : groups) {
			if (group.size() > 1) {
				// the first event has the first start date, but we don't know about the end date, so find the latest
				// end date (null is not-yet-closed and therefore always the latest)
				ParticipantDataEventValue event = (ParticipantDataEventValue) group.get(0).getData().get(eventColumnName);
				for (int i = 1; i < group.size(); i++) {
					ParticipantDataEventValue event2 = (ParticipantDataEventValue) group.get(i).getData().get(eventColumnName);
					if (event.getEnd() != null) {
						if (event2.getEnd() == null) {
							event.setEnd(null);
						} else {
							event.setEnd(Math.max(event.getEnd().longValue(), event2.getEnd().longValue()));
						}
					}
				}
			}
			result.add(group.get(0));
		}
		return result;
	}

	private List<List<ParticipantDataRow>> presortRowsGroupAndDate(List<ParticipantDataRow> rowList, final String eventColumnName) {
		// sort timeseries by grouping first and date second
		Collections.sort(rowList, new EventComparator(eventColumnName) {
			@Override
			public int compare(ParticipantDataEventValue e1, ParticipantDataEventValue e2) {
				String grouping1 = e1.getGrouping();
				String grouping2 = e2.getGrouping();
				if (grouping1 == null) {
					return grouping2 == null ? 0 : 1;
				}
				if (grouping2 == null) {
					return -1;
				}
				int comp = grouping1.compareTo(grouping2);
				if (comp != 0) {
					return comp;
				}
				return e1.getStart().compareTo(e2.getStart());
			}
		});

		// make a list of groups
		String lastGroup = null;
		int lastGroupStart = -1;
		List<List<ParticipantDataRow>> groups = Lists.newArrayList();
		for (int i = 0; i < rowList.size(); i++) {
			ParticipantDataEventValue event = (ParticipantDataEventValue) rowList.get(i).getData().get(eventColumnName);
			if (lastGroup == null || event.getGrouping() == null || !lastGroup.equals(event.getGrouping())) {
				if (lastGroupStart >= 0) {
					groups.add(rowList.subList(lastGroupStart, i));
				}
				lastGroup = event.getGrouping();
				lastGroupStart = i;
			}
		}
		if (lastGroupStart >= 0) {
			groups.add(rowList.subList(lastGroupStart, rowList.size()));
		}

		// sort by event start
		Collections.sort(groups, new Comparator<List<ParticipantDataRow>>() {
			@Override
			public int compare(List<ParticipantDataRow> group1, List<ParticipantDataRow> group2) {
				ParticipantDataEventValue event1 = (ParticipantDataEventValue) group1.get(0).getData().get(eventColumnName);
				ParticipantDataEventValue event2 = (ParticipantDataEventValue) group2.get(0).getData().get(eventColumnName);
				return event1.getStart().compareTo(event2.getStart());
			}
		});

		return groups;
	}

	private Double min(Double one, Double two) {
		if (one == null) {
			return two;
		}
		if (two == null) {
			return one;
		}
		return Math.min(one.doubleValue(), two.doubleValue());
	}

	private Double max(Double one, Double two) {
		if (one == null) {
			return two;
		}
		if (two == null) {
			return one;
		}
		return Math.max(one.doubleValue(), two.doubleValue());
	}
}
