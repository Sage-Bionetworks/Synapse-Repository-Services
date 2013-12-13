package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
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
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;

import com.google.common.collect.Lists;

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
			} catch (SynapseNotFoundException e) { }
		}
	}
	@After
	public void after() throws Exception {
	}

	
	@AfterClass
	public static void afterClass() throws Exception {
		for (Long id : usersToDelete) {
			//TODO This delete should not need to be surrounded by a try-catch
			// This means proper cleanup was not done by the test 
			try {
				adminSynapse.deleteUser(id);
			} catch (Exception e) { }
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
		participantDataDescriptor = bridge.createParticipantData(participantDataDescriptor);
		
		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumn(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumn(participantDataColumnDescriptor2);

		List<String> headers = Lists.newArrayList("level", "size");

		RowSet data1 = new RowSet();
		data1.setHeaders(headers);
		Row row = new Row();
		row.setValues(Lists.newArrayList("5", "200"));
		data1.setRows(Collections.singletonList(row));
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		RowSet data2 = new RowSet();
		data2.setHeaders(headers);
		Row row1 = new Row();
		row1.setValues(Lists.newArrayList("7", "250"));
		Row row2 = new Row();
		row2.setValues(Lists.newArrayList("3", "300"));
		data2.setRows(Lists.newArrayList(row1, row2));
		data2 = bridge.appendParticipantData(participantDataDescriptor.getId(), data2);

		RowSet data3 = new RowSet();
		data3.setHeaders(headers);
		Row row3 = new Row();
		row3.setValues(Lists.newArrayList("5", "200"));
		data3.setRows(Collections.singletonList(row3));
		data3 = bridgeTwo.appendParticipantData(participantDataDescriptor.getId(), data3);

		RowSet one = bridge.getParticipantData(participantDataDescriptor.getId());
		RowSet two = bridgeTwo.getParticipantData(participantDataDescriptor.getId());

		assertEquals(3, one.getRows().size());
		assertEquals(1, two.getRows().size());

		assertEquals(data3, two);
	}

	@Test
	public void testReplaceParticipantData() throws Exception {
		ParticipantDataDescriptor participantDataDescriptor = new ParticipantDataDescriptor();
		participantDataDescriptor.setName("my-first-participantData-" + System.currentTimeMillis());
		participantDataDescriptor = bridge.createParticipantData(participantDataDescriptor);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor1 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor1.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor1.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor1.setName("level");
		bridge.createParticipantDataColumn(participantDataColumnDescriptor1);

		ParticipantDataColumnDescriptor participantDataColumnDescriptor2 = new ParticipantDataColumnDescriptor();
		participantDataColumnDescriptor2.setParticipantDataDescriptorId(participantDataDescriptor.getId());
		participantDataColumnDescriptor2.setColumnType(ParticipantDataColumnType.STRING);
		participantDataColumnDescriptor2.setName("size");
		bridge.createParticipantDataColumn(participantDataColumnDescriptor2);

		List<String> headers = Lists.newArrayList("level", "size");

		RowSet data1 = new RowSet();
		data1.setHeaders(headers);
		Row row1 = new Row();
		row1.setValues(Lists.newArrayList("5", "200"));
		Row row2 = new Row();
		row2.setValues(Lists.newArrayList("6", "200"));
		Row row3 = new Row();
		row3.setValues(Lists.newArrayList("7", "200"));
		data1.setRows(Lists.newArrayList(row1, row2, row3));
		data1 = bridge.appendParticipantData(participantDataDescriptor.getId(), data1);

		RowSet dataToReplace = new RowSet();
		dataToReplace.setHeaders(headers);
		Row row2a = data1.getRows().get(1);
		row2a.getValues().set(0, "8");
		dataToReplace.setRows(Collections.singletonList(row2a));
		dataToReplace = bridge.updateParticipantData(participantDataDescriptor.getId(), dataToReplace);
		assertEquals(1, dataToReplace.getRows().size());
		assertEquals(data1.getRows().get(1).getRowId(), dataToReplace.getRows().get(0).getRowId());

		RowSet result = bridge.getParticipantData(participantDataDescriptor.getId());

		assertEquals(3, result.getRows().size());
		assertEquals("5", result.getRows().get(0).getValues().get(0));
		assertEquals("8", result.getRows().get(1).getValues().get(0));
		assertEquals("7", result.getRows().get(2).getValues().get(0));
	}
}

