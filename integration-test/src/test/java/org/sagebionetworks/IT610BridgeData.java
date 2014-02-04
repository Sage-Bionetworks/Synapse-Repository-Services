package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;
import org.sagebionetworks.bridge.model.data.ParticipantDataCurrentRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatusList;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataStringValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.versionInfo.BridgeVersionInfo;
import org.sagebionetworks.client.BridgeClient;
import org.sagebionetworks.client.BridgeClientImpl;
import org.sagebionetworks.client.BridgeProfileProxy;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.IdList;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Run this integration test as a sanity check to ensure our Synapse Java Client is working
 * 
 * @author deflaux
 */
public class IT610BridgeData {

	private static SynapseAdminClient adminSynapse;
	private static BridgeClient bridge = null;
	private static BridgeClient bridgeTwo = null;
	private static List<Long> usersToDelete;

	public static final int PREVIEW_TIMOUT = 10 * 1000;

	public static final int RDS_WORKER_TIMEOUT = 1000 * 60; // One min

	private List<String> handlesToDelete;

	private static BridgeClient createBridgeClient(SynapseAdminClient client) throws Exception {
		SynapseClient synapse = new SynapseClientImpl();
		usersToDelete.add(SynapseClientHelper.createUser(adminSynapse, synapse));

		BridgeClient bridge = new BridgeClientImpl(synapse);
		bridge.setBridgeEndpoint(StackConfiguration.getBridgeServiceEndpoint());

		// Return a proxy
		return BridgeProfileProxy.createProfileProxy(bridge);
	}

	private static SynapseClient createSynapse(BridgeClient bridge) {
		SynapseClient synapse = new SynapseClientImpl(bridge);
		SynapseClientHelper.setEndpoints(synapse);
		return synapse;
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		usersToDelete = new ArrayList<Long>();

		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());

		bridge = createBridgeClient(adminSynapse);
		bridgeTwo = createBridgeClient(adminSynapse);
	}

	@Before
	public void before() throws SynapseException {
		handlesToDelete = new ArrayList<String>();

		for (String id : handlesToDelete) {
			try {
				adminSynapse.deleteFileHandle(id);
			} catch (SynapseNotFoundException e) {
			}
		}
	}

	@After
	public void after() throws Exception {
	}

	@AfterClass
	public static void afterClass() throws Exception {
		for (Long id : usersToDelete) {
			// TODO This delete should not need to be surrounded by a try-catch
			// This means proper cleanup was not done by the test
			try {
				adminSynapse.deleteUser(id);
			} catch (Exception e) {
			}
		}
	}

	@Test
	public void testGetVersion() throws Exception {
		BridgeVersionInfo versionInfo = bridge.getBridgeVersionInfo();
		assertFalse(versionInfo.getVersion().isEmpty());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateAndDeleteParticipantData() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = bridge.createParticipantDataDescriptor(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor2);

		String[] headers = { "level", "size" };

		List<ParticipantDataRow> data1 = createRows(headers, null, "5", "200");
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		List<ParticipantDataRow> data2 = createRows(headers, null, "7", "250", null, "3", "300");
		data2 = bridge.appendParticipantData(participantDataDescriptor.getId(), data2);

		List<ParticipantDataRow> data3 = createRows(headers, null, "5", "200");
		data3 = bridgeTwo.appendParticipantData(participantDataDescriptor.getId(), data3);

		PaginatedResults<ParticipantDataRow> one = bridge.getRawParticipantData(participantDataDescriptor.getId(), Integer.MAX_VALUE, 0);
		PaginatedResults<ParticipantDataRow> two = bridgeTwo.getRawParticipantData(participantDataDescriptor.getId(), Integer.MAX_VALUE, 0);

		assertEquals(3, one.getResults().size());
		assertEquals(1, two.getResults().size());

		assertEquals(data3, two.getResults());
	}

	@Test
	public void testPaginatedParticipantData() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = bridge.createParticipantDataDescriptor(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor2);

		String[] headers = { "level", "size" };

		List<ParticipantDataRow> data1 = createRows(headers, null, "5", "200", null, "6", "200", null, "7", "200");
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		PaginatedResults<ParticipantDataRow> result;
		result = bridge.getRawParticipantData(participantDataDescriptor.getId(), 1, 0);
		assertEquals(1, result.getResults().size());
		assertEquals("5", ((ParticipantDataStringValue) result.getResults().get(0).getData().get("level")).getValue());

		result = bridge.getRawParticipantData(participantDataDescriptor.getId(), 1, 1);
		assertEquals(1, result.getResults().size());
		assertEquals("6", ((ParticipantDataStringValue) result.getResults().get(0).getData().get("level")).getValue());

		result = bridge.getRawParticipantData(participantDataDescriptor.getId(), 10, 1);
		assertEquals(2, result.getResults().size());
		assertEquals("6", ((ParticipantDataStringValue) result.getResults().get(0).getData().get("level")).getValue());
		assertEquals("7", ((ParticipantDataStringValue) result.getResults().get(1).getData().get("level")).getValue());
	}

	@Test
	public void testReplaceParticipantData() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = bridge.createParticipantDataDescriptor(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor2);

		String[] headers = { "level", "size" };

		List<ParticipantDataRow> data1 = createRows(headers, null, "5", "200", null, "6", "200", null, "7", "200");
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		List<ParticipantDataRow> dataToReplace = createRows(headers, data1.get(1).getRowId(), "8", "200");
		dataToReplace = bridge.updateParticipantData(participantDataDescriptor.getId(), dataToReplace);

		PaginatedResults<ParticipantDataRow> result = bridge.getRawParticipantData(participantDataDescriptor.getId(), Integer.MAX_VALUE, 0);

		assertEquals(3, result.getResults().size());
		assertEquals("5", ((ParticipantDataStringValue) result.getResults().get(0).getData().get("level")).getValue());
		assertEquals("8", ((ParticipantDataStringValue) result.getResults().get(1).getData().get("level")).getValue());
		assertEquals("7", ((ParticipantDataStringValue) result.getResults().get(2).getData().get("level")).getValue());
	}

	@Test
	@Ignore
	public void testGetCurrentParticipantData() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = bridge.createParticipantDataDescriptor(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor2);

		String[] headers = { "level", "size" };

		ParticipantDataStatusList statuses = new ParticipantDataStatusList();
		ParticipantDataStatus update = new ParticipantDataStatus();
		update.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		List<ParticipantDataStatus> updates = Collections.singletonList(update);
		statuses.setUpdates(updates);

		List<ParticipantDataRow> data1 = createRows(headers, null, "5", "200", null, "6", "200", null, "7", "200");
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		update.setLastEntryComplete(true);
		bridge.sendParticipantDataDescriptorUpdates(statuses);

		ParticipantDataCurrentRow currentRow = bridge.getCurrentParticipantData(participantDataDescriptor.getId());
		assertTrue(currentRow.getCurrentData().getData().isEmpty());
		assertEquals("7", ((ParticipantDataStringValue) currentRow.getPreviousData().getData().get("level")).getValue());

		update.setLastEntryComplete(false);
		bridge.sendParticipantDataDescriptorUpdates(statuses);

		currentRow = bridge.getCurrentParticipantData(participantDataDescriptor.getId());
		assertEquals("6", ((ParticipantDataStringValue) currentRow.getPreviousData().getData().get("level")).getValue());
		assertEquals("7", ((ParticipantDataStringValue) currentRow.getCurrentData().getData().get("level")).getValue());

		List<ParticipantDataRow> dataToReplace = createRows(headers, data1.get(2).getRowId(), "8", "200");
		dataToReplace = bridge.updateParticipantData(participantDataDescriptor.getId(), dataToReplace);

		currentRow = bridge.getCurrentParticipantData(participantDataDescriptor.getId());
		assertEquals("6", ((ParticipantDataStringValue) currentRow.getPreviousData().getData().get("level")).getValue());
		assertEquals("8", ((ParticipantDataStringValue) currentRow.getCurrentData().getData().get("level")).getValue());

		List<ParticipantDataRow> data3 = createRows(headers, null, "9", "200");
		data3 = bridge.appendParticipantData(participantDataDescriptor.getId(), data3);

		currentRow = bridge.getCurrentParticipantData(participantDataDescriptor.getId());
		assertEquals("8", ((ParticipantDataStringValue) currentRow.getPreviousData().getData().get("level")).getValue());
		assertEquals("9", ((ParticipantDataStringValue) currentRow.getCurrentData().getData().get("level")).getValue());

		update.setLastEntryComplete(true);
		bridge.sendParticipantDataDescriptorUpdates(statuses);

		currentRow = bridge.getCurrentParticipantData(participantDataDescriptor.getId());
		assertTrue(currentRow.getCurrentData().getData().isEmpty());
		assertEquals("9", ((ParticipantDataStringValue) currentRow.getPreviousData().getData().get("level")).getValue());
	}
	
	@Test
	public void testDeleteParticipantDataRows() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		participantDataDescriptor = bridge.createParticipantDataDescriptor(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumnDescriptor(participantDataColumnDescriptor1);

		String[] headers = { "level" };

		List<ParticipantDataRow> data1 = createRows(headers, null, "5", null, "200", null, "6");
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		List<Long> rowIds = Lists.newArrayListWithCapacity(data1.size());
		for (ParticipantDataRow row : data1) {
			rowIds.add(row.getRowId());
		}
		IdList idList = new IdList();
		idList.setList(rowIds);
		bridge.deleteParticipantDataRows(participantDataDescriptor.getId(), idList);
		
		PaginatedResults<ParticipantDataRow> result = bridge.getRawParticipantData(participantDataDescriptor.getId(), 1000, 0);
		assertEquals(0, result.getResults().size());
	}

	private List<ParticipantDataRow> createRows(String[] headers, Object... values) {
		List<ParticipantDataRow> data = Lists.newArrayList();
		for (int i = 0; i < values.length; i += headers.length + 1) {
			ParticipantDataRow row = new ParticipantDataRow();
			row.setData(Maps.<String, ParticipantDataValue> newHashMap());
			row.setRowId((Long) values[i]);
			data.add(row);
			for (int j = 0; j < headers.length; j++) {
				String value = (String) values[i + j + 1];
				ParticipantDataStringValue stringValue = new ParticipantDataStringValue();
				stringValue.setValue(value);
				row.getData().put(headers[j], stringValue);
			}
		}
		return data;
	}
}
