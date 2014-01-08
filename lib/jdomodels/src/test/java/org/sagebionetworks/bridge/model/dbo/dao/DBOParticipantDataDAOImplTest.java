package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipant;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOParticipantDataDAOImplTest extends TestBase {

	@Autowired
	private ParticipantDataDAO participantDataDAO;

	@Autowired
	private ParticipantDataDescriptorDAO participantDataDescriptorDAO;

	@Autowired
	private BridgeParticipantDAO participantDAO;

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private IdGenerator idGenerator;

	private DBOParticipant createParticipant() throws Exception {
		Long id = idGenerator.generateNewId();
		participantDAO.create(id.toString());
		addToDelete(DBOParticipant.class, id);
		DBOParticipant participant = new DBOParticipant();
		participant.setParticipantId(id);
		return participant;
	}

	private DBOParticipantDataDescriptor createModel() throws Exception {
		DBOParticipantDataDescriptor model = new DBOParticipantDataDescriptor();
		model.setId(idGenerator.generateNewId());
		model.setName("test-model" + model.getId().toString());
		model.setRepeatType(ParticipantDataRepeatType.ALWAYS);
		model = dboBasicDao.createNew(model);
		addToDelete(DBOParticipantDataDescriptor.class, model.getId().toString());
		return model;
	}

	private RowSet createRowSet(String[] columns, String[]... rows) {
		RowSet rowSet = new RowSet();
		rowSet.setHeaders(Arrays.asList(columns));
		rowSet.setRows(Lists.<Row> newArrayList());
		for (String[] rowdata : rows) {
			Row row = new Row();
			row.setRowId(rowdata[0] == null ? null : Long.parseLong(rowdata[0]));
			row.setValues(Lists.newArrayList(rowdata).subList(1, rowdata.length));
			rowSet.getRows().add(row);
		}
		return rowSet;
	}

	class DataDeletable extends Deletable {

		private Object participantDataId;
		private Object participantId;

		public DataDeletable(Object participantId, Object participantDataId) {
			super(null, null);
			this.participantId = participantId;
			this.participantDataId = participantDataId;
		}
	}

	@Override
	protected void doDelete(Deletable item) throws Exception {
		if (item instanceof DataDeletable) {
			try {
				participantDataDAO.get(((DataDeletable) item).participantId.toString(), ((DataDeletable) item).participantDataId.toString());
			} catch (NotFoundException e) {
				return;
			}
			participantDataDAO.delete(((DataDeletable) item).participantId.toString(), ((DataDeletable) item).participantDataId.toString());
		} else {
			super.doDelete(item);
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" },
				new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet);

		RowSet newRowSet = participantDataDAO.get(participant.getParticipantId().toString(), model.getId().toString());

		RowSet expectedRowSet = createRowSet(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "4",
				"5", "6" });
		assertEquals(expectedRowSet, newRowSet);
	}

	@Test
	public void testAppend() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet1);

		RowSet rowSet2 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "7", "8", "9" });

		participantDataDAO.update(participant.getParticipantId().toString(), model.getId().toString(), rowSet2);

		RowSet newRowSet = participantDataDAO.get(participant.getParticipantId().toString(), model.getId().toString());
		RowSet expectedRowSet = createRowSet(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "4",
				"5", "6" }, new String[] { "2", "7", "8", "9" });
		assertEquals(expectedRowSet, newRowSet);
	}

	@Test
	public void testReplace() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" }, new String[] { null, "7", "8", "9" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet1);

		RowSet rowSet2 = createRowSet(new String[] { "a", "b", "c" }, new String[] { "1", "14", "15", "16" });

		participantDataDAO.update(participant.getParticipantId().toString(), model.getId().toString(), rowSet2);

		RowSet newRowSet = participantDataDAO.get(participant.getParticipantId().toString(), model.getId().toString());
		RowSet expectedRowSet = createRowSet(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "14",
				"15", "16" }, new String[] { "2", "7", "8", "9" });
		assertEquals(expectedRowSet, newRowSet);
	}

	@Test
	public void testTotalReplace() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" }, new String[] { null, "7", "8", "9" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet1);

		RowSet rowSet2 = createRowSet(new String[] { "a", "b", "c" }, new String[] { "0", "1a", "2b", "3c" }, new String[] { "1", "4a", "5b",
				"6c" }, new String[] { "2", "7a", "8b", "9c" });

		participantDataDAO.update(participant.getParticipantId().toString(), model.getId().toString(), rowSet2);

		RowSet newRowSet = participantDataDAO.get(participant.getParticipantId().toString(), model.getId().toString());
		RowSet expectedRowSet = createRowSet(new String[] { "a", "b", "c" }, new String[] { "0", "1a", "2b", "3c" }, new String[] { "1",
				"4a", "5b", "6c" }, new String[] { "2", "7a", "8b", "9c" });
		assertEquals(expectedRowSet, newRowSet);
	}

	@Test
	public void testSparseAppend() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet1);

		RowSet rowSet2 = createRowSet(new String[] { "d", "e", "f" }, new String[] { null, "14", "15", "16" });

		participantDataDAO.update(participant.getParticipantId().toString(), model.getId().toString(), rowSet2);

		RowSet newRowSet = participantDataDAO.get(participant.getParticipantId().toString(), model.getId().toString());

		RowSet expectedRowSet = createRowSet(new String[] { "a", "b", "c", "d", "e", "f" }, new String[] { "0", "1", "2", "3", null, null,
				null }, new String[] { "1", "4", "5", "6", null, null, null }, new String[] { "2", null, null, null, "14", "15", "16" });
		assertEquals(expectedRowSet, newRowSet);
	}

	@Test
	public void testGetParticipantDataParticipant() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));
		participantDataDAO.append(participant.getParticipantId().toString(), model.getId().toString(), rowSet1);

		String foundId;

		foundId = participantDataDAO.findParticipantForParticipantData(Lists.newArrayList(participant.getParticipantId().toString()), model
				.getId().toString());
		assertEquals(participant.getParticipantId().toString(), foundId);

		foundId = participantDataDAO.findParticipantForParticipantData(
				Lists.newArrayList("aaa", participant.getParticipantId().toString(), "bbb"), model.getId().toString());
		assertEquals(participant.getParticipantId().toString(), foundId);

		foundId = participantDataDAO.findParticipantForParticipantData(Lists.newArrayList("aaa", "bbb"), model.getId().toString());
		assertNull(foundId);
	}

	@Test
	public void testGetParticipantDatas() throws Exception {
		DBOParticipant participant1 = createParticipant();
		DBOParticipant participant2 = createParticipant();
		DBOParticipantDataDescriptor model1 = createModel();
		DBOParticipantDataDescriptor model2 = createModel();

		RowSet rowSet1 = createRowSet(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5",
				"6" });

		addToDelete(new DataDeletable(participant1.getParticipantId(), model1.getId().toString()));
		participantDataDAO.append(participant1.getParticipantId().toString(), model1.getId().toString(), rowSet1);

		addToDelete(new DataDeletable(participant1.getParticipantId(), model2.getId().toString()));
		participantDataDAO.append(participant1.getParticipantId().toString(), model2.getId().toString(), rowSet1);

		addToDelete(new DataDeletable(participant2.getParticipantId(), model2.getId().toString()));
		participantDataDAO.append(participant2.getParticipantId().toString(), model2.getId().toString(), rowSet1);

		List<ParticipantDataDescriptor> foundParticipantDatas;

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDatasForUser(Lists.newArrayList(participant1.getParticipantId()
				.toString()));
		assertEquals(2, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDatasForUser(Lists.newArrayList("aaa", participant1
				.getParticipantId().toString(), "bbb"));
		assertEquals(2, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDatasForUser(Lists.newArrayList("aaa", "bbb"));
		assertEquals(0, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDatasForUser(Lists.newArrayList(participant1.getParticipantId()
				.toString(), participant2.getParticipantId().toString()));
		assertEquals(3, foundParticipantDatas.size());
	}
}
