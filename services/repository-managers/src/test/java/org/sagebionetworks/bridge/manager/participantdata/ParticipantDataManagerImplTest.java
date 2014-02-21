package org.sagebionetworks.bridge.manager.participantdata;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sagebionetworks.bridge.manager.community.MockitoTestBase;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.BridgeUserParticipantMappingDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDatetimeValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.data.value.ValueFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

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
	public void testDateFiltering() throws Exception {
		ParticipantDataManagerImpl participantDataManagerImpl = new ParticipantDataManagerImpl(participantDataDAO,
				participantDataMappingManager, participantDataStatusDAO, participantDataDescriptionManager);

		UserInfo userInfo = new UserInfo(false, 1009L);

		Long one = 1L;
		Long two = 3L;
		Long three = 5L;
		Long four = 7L;
		ParticipantDataRow row1 = new ParticipantDataRow();
		row1.setData(createMap("start", ValueFactory.createDatetimeValue(one)));
		ParticipantDataRow row2 = new ParticipantDataRow();
		row2.setData(createMap("start", ValueFactory.createDatetimeValue(two)));
		ParticipantDataRow row3 = new ParticipantDataRow();
		row3.setData(createMap("start", ValueFactory.createDatetimeValue(three)));
		ParticipantDataRow row4 = new ParticipantDataRow();
		row4.setData(createMap("start", ValueFactory.createDatetimeValue(four)));

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setDatetimeStartColumnName("start");
		participantDataDescriptor.setDatetimeEndColumnName("end");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				Lists.newArrayList(row3, row1, row4, row2));

		List<ParticipantDataRow> historyData;

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, null);
		assertEquals(4, historyData.size());
		assertTrue(getStart(historyData, 0) < getStart(historyData, 1));
		assertTrue(getStart(historyData, 1) < getStart(historyData, 2));
		assertTrue(getStart(historyData, 2) < getStart(historyData, 3));

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(0L), null);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(1L), null);
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(2L), null);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), null);
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(6L), null);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(7L), null);
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(8L), null);
		assertEquals(0, historyData.size());

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(8L));
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(7L));
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(6L));
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(5L));
		assertEquals(3, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(2L));
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(1L));
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, null, new Date(0L));
		assertEquals(0, historyData.size());

		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(0L), new Date(8L));
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(1L), new Date(7L));
		assertEquals(4, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(2L), new Date(6L));
		assertEquals(2, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), new Date(5L));
		assertEquals(2, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(3L), new Date(4L));
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(5L));
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(5L), new Date(5L));
		assertEquals(1, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(4L));
		assertEquals(0, historyData.size());
		historyData = participantDataManagerImpl.getHistoryData(userInfo, "20", false, new Date(4L), new Date(1L));
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
		row1.setData(createMap("start", ValueFactory.createDatetimeValue(one), "end", null));
		ParticipantDataRow row2 = new ParticipantDataRow();
		row2.setData(createMap("start", ValueFactory.createDatetimeValue(two), "end", ValueFactory.createDatetimeValue(four)));
		ParticipantDataRow row3 = new ParticipantDataRow();
		row3.setData(createMap("start", ValueFactory.createDatetimeValue(three), "end", ValueFactory.createDatetimeValue(four)));
		ParticipantDataRow row4 = new ParticipantDataRow();
		row4.setData(createMap("start", ValueFactory.createDatetimeValue(four), "end", null));

		when(participantDataDAO.findParticipantForParticipantData(anyList(), anyString())).thenReturn(new ParticipantDataId(0));
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setDatetimeStartColumnName("start");
		participantDataDescriptor.setDatetimeEndColumnName("end");
		when(participantDataDescriptionManager.getParticipantDataDescriptor(userInfo, "20")).thenReturn(participantDataDescriptor);
		when(participantDataDAO.get(new ParticipantDataId(0), "20", Collections.<ParticipantDataColumnDescriptor> emptyList())).thenReturn(
				Lists.newArrayList(row3, row1, row4, row2));

		List<ParticipantDataRow> currentData;

		currentData = participantDataManagerImpl.getHistoryData(userInfo, "20", true, null, null);
		assertEquals(2, currentData.size());
		assertEquals(1L, getStart(currentData, 0).longValue());
		assertEquals(7L, getStart(currentData, 1).longValue());
	}

	private Map<String, ParticipantDataValue> createMap(Object... keyValues) {
		Map<String, ParticipantDataValue> result = Maps.newHashMap();
		for (int i = 0; i < keyValues.length; i += 2) {
			result.put((String) keyValues[i], (ParticipantDataValue) keyValues[i + 1]);
		}
		return result;
	}

	private Long getStart(List<ParticipantDataRow> historyData, int index) {
		return ((ParticipantDataDatetimeValue) historyData.get(index).getData().get("start")).getValue();
	}
}
