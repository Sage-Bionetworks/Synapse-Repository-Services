package org.sagebionetworks.bridge.manager.participantdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.sagebionetworks.bridge.manager.community.MockitoTestBase;
import org.sagebionetworks.bridge.manager.participantdata.ParticipantDataManager.SortType;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDoubleValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataEventValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLabValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.data.value.ValueFactory;
import org.sagebionetworks.bridge.model.timeseries.TimeSeriesTable;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ParticipantDataManagerImplTest extends MockitoTestBase {

	@Mock
	private ParticipantDataDAO participantDataDAO;
	@Mock
	private ParticipantDataIdMappingManager participantDataMappingManager;
	@Mock
	private ParticipantDataStatusDAO participantDataStatusDAO;
	@Mock
	private ParticipantDataDescriptionManager participantDataDescriptionManager;

	@Before
	public void doBefore() {
		initMockito(false);
	}
	
	@Test 
	public void getDataNormalizationWorks() throws Exception {
		ParticipantDataManagerImpl manager = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);
		
		UserInfo userInfo = new UserInfo(false, 1009L);
		
		ParticipantDataRow row = new ParticipantDataRow();
		row.setData(new HashMap<String,ParticipantDataValue>());
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// Mock out retrieval of the participant ID or no rows will be returned.
		when(participantDataMappingManager.mapSynapseUserToParticipantIds(any(UserInfo.class))).thenReturn(
			Lists.newArrayList(new ParticipantDataId(1009L)));
		when(participantDataDAO.findParticipantForParticipantData(Matchers.anyListOf(ParticipantDataId.class),
			any(String.class))).thenReturn(new ParticipantDataId(1009L));
		
		// Actual calls to retrieve the rows
		when(participantDataDAO.get(any(ParticipantDataId.class), anyString(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(Lists.newArrayList(row));
		when(participantDataDAO.getRow(any(ParticipantDataId.class), anyString(), anyLong(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(row);
		
		// This is converted
		PaginatedResults<ParticipantDataRow> results = manager.getData(userInfo, "20", 100, 0, true);
		ParticipantDataRow convertedRow = results.getResults().get(0);
		
		ParticipantDataLabValue lab = (ParticipantDataLabValue)convertedRow.getData().get("lab");
		assertEquals(.9, lab.getValue(), 0.0);
		assertEquals("L", lab.getUnits());
		assertEquals(5, lab.getMinNormal(), 0.0);
		assertEquals(10, lab.getMaxNormal(), 0.0);
		
		ParticipantDataDoubleValue d = (ParticipantDataDoubleValue)convertedRow.getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
		
		// RESET
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// This is not converted
		results = manager.getData(userInfo, "20", 100, 0, false);
		ParticipantDataRow unConvertedRow = results.getResults().get(0);
		lab = (ParticipantDataLabValue)unConvertedRow.getData().get("lab");
		assertEquals(9, lab.getValue(), 0.0);
		assertEquals("dL", lab.getUnits());
		assertEquals(50, lab.getMinNormal(), 0.0);
		assertEquals(100, lab.getMaxNormal(), 0.0);
		
		d = (ParticipantDataDoubleValue)unConvertedRow.getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
	}
	
	@Test 
	public void getHistoryDataNormalizationWorks() throws Exception {
		ParticipantDataManagerImpl manager = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);
		
		ParticipantDataDescriptor descriptor = new ParticipantDataDescriptor();
		descriptor.setEventColumnName("foo");
		
		ParticipantDataEventValue event = new ParticipantDataEventValue();
		event.setStart(0L);
		event.setEnd(new Date().getTime());
		
		UserInfo userInfo = new UserInfo(false, 1009L);
		
		ParticipantDataRow row = new ParticipantDataRow();
		row.setData(new HashMap<String,ParticipantDataValue>());
		row.getData().put("foo", event);
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// Mock out retrieval of the participant ID or no rows will be returned.
		when(participantDataMappingManager.mapSynapseUserToParticipantIds(any(UserInfo.class))).thenReturn(
				Lists.newArrayList(new ParticipantDataId(1009L)));
		when(
				participantDataDAO.findParticipantForParticipantData(Matchers.anyListOf(ParticipantDataId.class),
						any(String.class))).thenReturn(new ParticipantDataId(1009L));
		
		// Actual calls to retrieve the rows
		when(participantDataDAO.get(any(ParticipantDataId.class), anyString(),
				Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(Lists.newArrayList(row));

		when(participantDataDAO.getRow(any(ParticipantDataId.class), anyString(), anyLong(),
				Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(row);
		
		when(participantDataDescriptionManager.getParticipantDataDescriptor(any(UserInfo.class), any(String.class))).thenReturn(descriptor);
		
		// This is converted
		List<ParticipantDataRow> results = manager.getHistoryData(userInfo, "20", false, new Date(0L), new Date(
				new Date().getTime() + 10000L), SortType.SORT_BY_DATE, true);
		ParticipantDataRow convertedRow = results.get(0);
		
		ParticipantDataLabValue lab = (ParticipantDataLabValue)convertedRow.getData().get("lab");
		assertEquals(.9, lab.getValue(), 0.0);
		assertEquals("L", lab.getUnits());
		assertEquals(5, lab.getMinNormal(), 0.0);
		assertEquals(10, lab.getMaxNormal(), 0.0);
		
		ParticipantDataDoubleValue d = (ParticipantDataDoubleValue)convertedRow.getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
		
		// RESET
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// This is not converted
		results = manager.getHistoryData(userInfo, "20", false, new Date(0L), new Date(
				new Date().getTime() + 10000L), SortType.SORT_BY_DATE, false);
		ParticipantDataRow unConvertedRow = results.get(0);
		lab = (ParticipantDataLabValue)unConvertedRow.getData().get("lab");
		assertEquals(9, lab.getValue(), 0.0);
		assertEquals("dL", lab.getUnits());
		assertEquals(50, lab.getMinNormal(), 0.0);
		assertEquals(100, lab.getMaxNormal(), 0.0);
		
		d = (ParticipantDataDoubleValue)unConvertedRow.getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
	}
	
	@Test
	public void getCurrentDataNormalizationWorks() throws Exception {
		ParticipantDataManagerImpl manager = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);
		
		UserInfo userInfo = new UserInfo(false, 1009L);
		
		ParticipantDataStatus status = new ParticipantDataStatus();
		status.setLastEntryComplete(Boolean.TRUE);
		
		ParticipantDataRow row = new ParticipantDataRow();
		row.setData(new HashMap<String,ParticipantDataValue>());
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// Mock out retrieval of the participant ID or no rows will be returned.
		when(participantDataMappingManager.mapSynapseUserToParticipantIds(any(UserInfo.class))).thenReturn(
			Lists.newArrayList(new ParticipantDataId(1009L)));
		when(participantDataDAO.findParticipantForParticipantData(Matchers.anyListOf(ParticipantDataId.class),
			any(String.class))).thenReturn(new ParticipantDataId(1009L));
		
		// Actual calls to retrieve the rows
		when(participantDataDAO.get(any(ParticipantDataId.class), anyString(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(Lists.newArrayList(row));
		when(participantDataDAO.getRow(any(ParticipantDataId.class), anyString(), anyLong(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(row);
		when(participantDataStatusDAO.getParticipantStatus(any(ParticipantDataId.class),
			any(ParticipantDataDescriptor.class))).thenReturn(status);
		
		// This is converted
		ParticipantDataCurrentRow convertedRow = manager.getCurrentData(userInfo, "20", true);
		
		ParticipantDataLabValue lab = (ParticipantDataLabValue)convertedRow.getPreviousData().getData().get("lab");
		assertEquals(.9, lab.getValue(), 0.0);
		assertEquals("L", lab.getUnits());
		assertEquals(5, lab.getMinNormal(), 0.0);
		assertEquals(10, lab.getMaxNormal(), 0.0);
		
		ParticipantDataDoubleValue d = (ParticipantDataDoubleValue)convertedRow.getPreviousData().getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
		
		// RESET
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// This is not converted
		ParticipantDataCurrentRow unConvertedRow = manager.getCurrentData(userInfo, "20", false);
		lab = (ParticipantDataLabValue)unConvertedRow.getPreviousData().getData().get("lab");
		assertEquals(9, lab.getValue(), 0.0);
		assertEquals("dL", lab.getUnits());
		assertEquals(50, lab.getMinNormal(), 0.0);
		assertEquals(100, lab.getMaxNormal(), 0.0);
		
		d = (ParticipantDataDoubleValue)unConvertedRow.getPreviousData().getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
	}
	
	@Test
	public void getDataRowNormalizationWorks() throws Exception {
		ParticipantDataManagerImpl manager = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);
		
		UserInfo userInfo = new UserInfo(false, 1009L);
		
		ParticipantDataStatus status = new ParticipantDataStatus();
		status.setLastEntryComplete(Boolean.TRUE);
		
		ParticipantDataRow row = new ParticipantDataRow();
		row.setData(new HashMap<String,ParticipantDataValue>());
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// Mock out retrieval of the participant ID or no rows will be returned.
		when(participantDataMappingManager.mapSynapseUserToParticipantIds(any(UserInfo.class))).thenReturn(
			Lists.newArrayList(new ParticipantDataId(1009L)));
		when(participantDataDAO.findParticipantForParticipantData(Matchers.anyListOf(ParticipantDataId.class),
			any(String.class))).thenReturn(new ParticipantDataId(1009L));
		
		// Actual calls to retrieve the rows
		when(participantDataDAO.get(any(ParticipantDataId.class), anyString(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(Lists.newArrayList(row));
		when(participantDataDAO.getRow(any(ParticipantDataId.class), anyString(), anyLong(),
			Matchers.anyListOf(ParticipantDataColumnDescriptor.class))).thenReturn(row);
		when(participantDataStatusDAO.getParticipantStatus(any(ParticipantDataId.class),
			any(ParticipantDataDescriptor.class))).thenReturn(status);
		
		// This is converted
		ParticipantDataRow convertedRow = manager.getDataRow(userInfo, "20", 1009L, true);
		
		ParticipantDataLabValue lab = (ParticipantDataLabValue)convertedRow.getData().get("lab");
		assertEquals(.9, lab.getValue(), 0.0);
		assertEquals("L", lab.getUnits());
		assertEquals(5, lab.getMinNormal(), 0.0);
		assertEquals(10, lab.getMaxNormal(), 0.0);
		
		ParticipantDataDoubleValue d = (ParticipantDataDoubleValue)convertedRow.getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
		
		// RESET
		row.getData().put("lab", ValueFactory.createLabValue(9, "dL", 50, 100));
		row.getData().put("double", ValueFactory.createDoubleValue(20d));
		
		// This is not converted
		ParticipantDataCurrentRow unConvertedRow = manager.getCurrentData(userInfo, "20", false);
		lab = (ParticipantDataLabValue)unConvertedRow.getPreviousData().getData().get("lab");
		assertEquals(9, lab.getValue(), 0.0);
		assertEquals("dL", lab.getUnits());
		assertEquals(50, lab.getMinNormal(), 0.0);
		assertEquals(100, lab.getMaxNormal(), 0.0);
		
		d = (ParticipantDataDoubleValue)unConvertedRow.getPreviousData().getData().get("double");
		assertEquals(20, d.getValue(), 0.0);
	}
	
	@Test
	public void testDateFiltering() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		Long one = 1L;
		Long two = 3L;
		Long three = 5L;
		Long four = 7L;
		ParticipantDataRow row1 = new ParticipantDataRow();
		row1.setData(createMap("event", ValueFactory.createEventValue(one, null, "a", null)));
		ParticipantDataRow row2 = new ParticipantDataRow();
		row2.setData(createMap("event", ValueFactory.createEventValue(two, null, "b", null)));
		ParticipantDataRow row3 = new ParticipantDataRow();
		row3.setData(createMap("event", ValueFactory.createEventValue(three, null, "c", null)));
		ParticipantDataRow row4 = new ParticipantDataRow();
		row4.setData(createMap("event", ValueFactory.createEventValue(four, null, "d", null)));

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setEventColumnName("event");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				Lists.newArrayList(row3, row1, row4, row2));

		List<ParticipantDataRow> historyData;

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, null, SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		assertTrue(getStart(historyData, 0) < getStart(historyData, 1));
		assertTrue(getStart(historyData, 1) < getStart(historyData, 2));
		assertTrue(getStart(historyData, 2) < getStart(historyData, 3));

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(0L), null, SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(1L), null, SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(2L), null, SortType.SORT_BY_DATE, false);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), null, SortType.SORT_BY_DATE, false);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(6L), null, SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(7L), null, SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(8L), null, SortType.SORT_BY_DATE, false);
		assertEquals(0, historyData.size());

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(8L), SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(7L), SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(6L), SortType.SORT_BY_DATE, false);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(5L), SortType.SORT_BY_DATE, false);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(2L), SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(1L), SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(0L), SortType.SORT_BY_DATE, false);
		assertEquals(0, historyData.size());

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(0L), new Date(8L), SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(1L), new Date(7L), SortType.SORT_BY_DATE, false);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(2L), new Date(6L), SortType.SORT_BY_DATE, false);
		assertEquals(2, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), new Date(5L), SortType.SORT_BY_DATE, false);
		assertEquals(2, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), new Date(4L), SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(5L), SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(5L), new Date(5L), SortType.SORT_BY_DATE, false);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(4L), SortType.SORT_BY_DATE, false);
		assertEquals(0, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(1L), SortType.SORT_BY_DATE, false);
		assertEquals(0, historyData.size());
	}

	@Test
	public void testCurrentFiltering() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		Long one = 1L;
		Long two = 3L;
		Long three = 5L;
		Long four = 7L;
		ParticipantDataRow row1 = new ParticipantDataRow();
		row1.setData(createMap("event", ValueFactory.createEventValue(one, null, "a", null)));
		ParticipantDataRow row2 = new ParticipantDataRow();
		row2.setData(createMap("event", ValueFactory.createEventValue(two, four, "b", null)));
		ParticipantDataRow row3 = new ParticipantDataRow();
		row3.setData(createMap("event", ValueFactory.createEventValue(three, four, "c", null)));
		ParticipantDataRow row4 = new ParticipantDataRow();
		row4.setData(createMap("event", ValueFactory.createEventValue(four, null, "d", null)));

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setEventColumnName("event");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				Lists.newArrayList(row3, row1, row4, row2));

		List<ParticipantDataRow> currentData = participantDataManagerImpl.getHistoryData(userInfo, "20", true, null, null,
				SortType.SORT_BY_DATE, false);
		assertEquals(2, currentData.size());
		assertEquals(1L, getStart(currentData, 0).longValue());
		assertEquals(7L, getStart(currentData, 1).longValue());
	}

	@Test
	public void testHistorySorting() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		List<ParticipantDataRow> rows = createDatas(4L, 10L, null, 3L, 10L, "aa", 2L, 10L, "aa", 1L, 10L, null, 3L, 10L, null, 2L, 10L, "bb",
				1L, 10L, "aa", 3L, 10L, "bb");

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setEventColumnName("event");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				rows);

		List<ParticipantDataRow> currentData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, null,
				SortType.SORT_BY_GROUP_AND_DATE, false);
		assertEquals(8, currentData.size());
		int index = 0;
		assertEquals(1L, getStart(currentData, index).longValue());
		assertEquals("aa", getGroup(currentData, index++));
		assertEquals(2L, getStart(currentData, index).longValue());
		assertEquals("aa", getGroup(currentData, index++));
		assertEquals(3L, getStart(currentData, index).longValue());
		assertEquals("aa", getGroup(currentData, index++));
		assertEquals(1L, getStart(currentData, index).longValue());
		assertEquals(null, getGroup(currentData, index++));
		assertEquals(2L, getStart(currentData, index).longValue());
		assertEquals("bb", getGroup(currentData, index++));
		assertEquals(3L, getStart(currentData, index).longValue());
		assertEquals("bb", getGroup(currentData, index++));
		assertEquals(3L, getStart(currentData, index).longValue());
		assertEquals(null, getGroup(currentData, index++));
		assertEquals(4L, getStart(currentData, index).longValue());
		assertEquals(null, getGroup(currentData, index++));
	}

	@Test
	public void testTimelineGroupingAndSorting() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		List<ParticipantDataRow> rows = createDatas(4L, null, null, 3L, 1L, "aa", 2L, null, "aa", 1L, 10L, null, 3L, 10L, null, 2L, 9L, "bb",
				1L, 10L, "aa", 3L, 10L, "bb");

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setEventColumnName("event");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				rows);

		TimeSeriesTable currentData = participantDataManagerImpl.getTimeSeries(userInfo, "20", null, false);
		assertEquals(5, currentData.getEvents().size());
		int index = 0;
		assertEquals(1L, currentData.getEvents().get(index).getStart().longValue());
		assertEquals(null, currentData.getEvents().get(index).getEnd());
		assertEquals("aa", currentData.getEvents().get(index++).getGrouping());

		assertEquals(1L, currentData.getEvents().get(index).getStart().longValue());
		assertEquals(10L, currentData.getEvents().get(index).getEnd().longValue());
		assertEquals(null, currentData.getEvents().get(index++).getGrouping());

		assertEquals(2L, currentData.getEvents().get(index).getStart().longValue());
		assertEquals(10L, currentData.getEvents().get(index).getEnd().longValue());
		assertEquals("bb", currentData.getEvents().get(index++).getGrouping());

		assertEquals(3L, currentData.getEvents().get(index).getStart().longValue());
		assertEquals(10L, currentData.getEvents().get(index).getEnd().longValue());
		assertEquals(null, currentData.getEvents().get(index++).getGrouping());

		assertEquals(4L, currentData.getEvents().get(index).getStart().longValue());
		assertEquals(null, currentData.getEvents().get(index).getEnd());
		assertEquals(null, currentData.getEvents().get(index++).getGrouping());
	}

	private List<ParticipantDataRow> createDatas(Object... data) {
		ArrayList<ParticipantDataRow> result = Lists.newArrayList();
		for (int i = 0; i < data.length; i += 3) {
			ParticipantDataRow row = new ParticipantDataRow();
			row.setData(createMap("event", ValueFactory.createEventValue((Long) data[i], (Long) data[i + 1], "i" + i, (String) data[i + 2])));
			result.add(row);
		}
		return result;
	}

	@Test
	public void testTimeseriesEvents() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		Long one = 1L;
		Long two = 3L;
		Long three = 5L;
		Long four = 7L;
		ParticipantDataRow row1 = new ParticipantDataRow();
		row1.setData(createMap("event", ValueFactory.createEventValue(one, two, "a", "aa")));
		ParticipantDataRow row2 = new ParticipantDataRow();
		row2.setData(createMap("event", ValueFactory.createEventValue(two, four, "b", "aa")));
		ParticipantDataRow row3 = new ParticipantDataRow();
		row3.setData(createMap("event", ValueFactory.createEventValue(three, null, "c", "aa")));
		ParticipantDataRow row4 = new ParticipantDataRow();
		row4.setData(createMap("event", ValueFactory.createEventValue(four, null, "d", "bb")));

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setDatetimeStartColumnName("start");
		participantDataDescriptor.setEventColumnName("event");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				Lists.newArrayList(row3, row1, row4, row2));

		TimeSeriesTable currentData;

		currentData = participantDataManagerImpl.getTimeSeries(userInfo, "20", null, false);
		assertEquals(2, currentData.getEvents().size());
		assertEquals(1L, currentData.getEvents().get(0).getStart().longValue());
		assertNull(currentData.getEvents().get(0).getEnd());
		assertEquals(7L, currentData.getEvents().get(1).getStart().longValue());
		assertNull(currentData.getEvents().get(1).getEnd());
	}

	private Map<String, ParticipantDataValue> createMap(Object... keyValues) {
		Map<String, ParticipantDataValue> result = Maps.newHashMap();
		for (int i = 0; i < keyValues.length; i += 2) {
			result.put((String) keyValues[i], (ParticipantDataValue) keyValues[i + 1]);
		}
		return result;
	}

	private Long getStart(List<ParticipantDataRow> historyData, int index) {
		return ((ParticipantDataEventValue) historyData.get(index).getData().get("event")).getStart();
	}

	private Long getEnd(List<ParticipantDataRow> historyData, int index) {
		return ((ParticipantDataEventValue) historyData.get(index).getData().get("event")).getEnd();
	}

	private String getGroup(List<ParticipantDataRow> historyData, int index) {
		return ((ParticipantDataEventValue) historyData.get(index).getData().get("event")).getGrouping();
	}
}
