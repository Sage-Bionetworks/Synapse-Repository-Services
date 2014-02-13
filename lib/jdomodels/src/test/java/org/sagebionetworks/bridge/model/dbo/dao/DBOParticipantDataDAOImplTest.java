package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.bridge.model.BridgeParticipantDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDAO;
import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnType;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataRepeatType;
import org.sagebionetworks.bridge.model.data.ParticipantDataRow;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataBooleanValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDatetimeValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataDoubleValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLabValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataLongValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataStringValue;
import org.sagebionetworks.bridge.model.data.value.ParticipantDataValue;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipant;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOParticipantDataDescriptor;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
		participantDAO.create(id);
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

	private static class Rows {
		List<ParticipantDataRow> rows = Lists.newArrayList();
		List<ParticipantDataColumnDescriptor> columns = Lists.newArrayList();
	}

	private Rows createRowList(String[] columns, String[]... rows) {
		Rows result = new Rows();
		for (String column : columns) {
			ParticipantDataColumnDescriptor columnDescriptor = new ParticipantDataColumnDescriptor();
			columnDescriptor.setColumnType(ParticipantDataColumnType.STRING);
			columnDescriptor.setName(column);
			result.columns.add(columnDescriptor);
		}
		for (String[] rowdata : rows) {
			ParticipantDataRow row = new ParticipantDataRow();
			row.setRowId(rowdata[0] == null ? null : Long.parseLong(rowdata[0]));
			Map<String, ParticipantDataValue> data = Maps.newHashMap();
			for (int i = 1; i < rowdata.length; i++) {
				if (rowdata[i] != null) {
					ParticipantDataStringValue stringValue = new ParticipantDataStringValue();
					stringValue.setValue(rowdata[i]);
					data.put(columns[i - 1], stringValue);
				}
			}
			row.setData(data);
			result.rows.add(row);
		}
		return result;
	}

	class DataDeletable extends Deletable {

		private Long participantId;
		private String participantDataDescriptorId;

		public DataDeletable(Long participantId, String participantDataDescriptorId) {
			super(null, null);
			this.participantId = participantId;
			this.participantDataDescriptorId = participantDataDescriptorId;
		}
	}

	@Override
	protected void doDelete(Deletable item) throws Exception {
		if (item instanceof DataDeletable) {
			try {
				participantDataDAO.delete(new ParticipantDataId(((DataDeletable) item).participantId),
						((DataDeletable) item).participantDataDescriptorId);
			} catch (NotFoundException e) {
				return;
			}
		} else {
			super.doDelete(item);
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows.rows, rows.columns);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.columns);

		Rows expectedRows = createRowList(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "4", "5",
				"6" });
		assertEquals(expectedRows.rows, newRowSet);
	}

	@Test
	public void testAppend() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows1.rows, rows1.columns);

		Rows rows2 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "7", "8", "9" });

		participantDataDAO.update(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows2.rows, rows2.columns);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows1.columns);
		Rows expectedRows = createRowList(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "4", "5",
				"6" }, new String[] { "2", "7", "8", "9" });
		assertEquals(expectedRows.rows, newRowSet);
	}

	@Test
	public void testReplace() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" },
				new String[] { null, "4", "5", "6" }, new String[] { null, "7", "8", "9" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows1.rows, rows1.columns);

		Rows rows2 = createRowList(new String[] { "a", "b", "c" }, new String[] { "1", "14", "15", "16" });

		participantDataDAO.update(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows2.rows, rows2.columns);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows1.columns);
		Rows expectedRows = createRowList(new String[] { "a", "b", "c" }, new String[] { "0", "1", "2", "3" }, new String[] { "1", "14",
				"15", "16" }, new String[] { "2", "7", "8", "9" });
		assertEquals(expectedRows.rows, newRowSet);
	}

	@Test
	public void testTotalReplace() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" },
				new String[] { null, "4", "5", "6" }, new String[] { null, "7", "8", "9" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows1.rows, rows1.columns);

		Rows rows2 = createRowList(new String[] { "a", "b", "c" }, new String[] { "0", "1a", "2b", "3c" }, new String[] { "1", "4a", "5b",
				"6c" }, new String[] { "2", "7a", "8b", "9c" });

		participantDataDAO.update(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows2.rows, rows2.columns);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows1.columns);
		Rows expectedRows = createRowList(new String[] { "a", "b", "c" }, new String[] { "0", "1a", "2b", "3c" }, new String[] { "1", "4a",
				"5b", "6c" }, new String[] { "2", "7a", "8b", "9c" });
		assertEquals(expectedRows.rows, newRowSet);
	}

	@Test
	public void testSparseAppend() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows1.rows, rows1.columns);

		Rows rows2 = createRowList(new String[] { "d", "e", "f" }, new String[] { null, "14", "15", "16" });

		participantDataDAO.update(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows2.rows, rows2.columns);

		List<ParticipantDataColumnDescriptor> allColumns = Lists.newArrayList(rows1.columns);
		allColumns.addAll(rows2.columns);
		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), allColumns);

		Rows expectedRows = createRowList(new String[] { "a", "b", "c", "d", "e", "f" },
				new String[] { "0", "1", "2", "3", null, null, null }, new String[] { "1", "4", "5", "6", null, null, null }, new String[] {
						"2", null, null, null, "14", "15", "16" });
		assertEquals(expectedRows.rows, newRowSet);
	}

	@Test
	public void testGetParticipantDataParticipant() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));
		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows1.rows, rows1.columns);

		ParticipantDataId foundId;

		foundId = participantDataDAO.findParticipantForParticipantData(
				Lists.newArrayList(new ParticipantDataId(participant.getParticipantId())), model.getId().toString());
		assertEquals(new ParticipantDataId(participant.getParticipantId()), foundId);

		foundId = participantDataDAO.findParticipantForParticipantData(Lists.newArrayList(new ParticipantDataId(-1), new ParticipantDataId(
				participant.getParticipantId()), new ParticipantDataId(-2)), model.getId().toString());
		assertEquals(new ParticipantDataId(participant.getParticipantId()), foundId);

		foundId = participantDataDAO.findParticipantForParticipantData(
				Lists.newArrayList(new ParticipantDataId(-1), new ParticipantDataId(-2)), model.getId().toString());
		assertNull(foundId);
	}

	@Test
	public void testGetParticipantDatas() throws Exception {
		DBOParticipant participant1 = createParticipant();
		DBOParticipant participant2 = createParticipant();
		DBOParticipant participant3 = createParticipant();
		DBOParticipantDataDescriptor model1 = createModel();
		DBOParticipantDataDescriptor model2 = createModel();

		Rows rows1 = createRowList(new String[] { "a", "b", "c" }, new String[] { null, "1", "2", "3" }, new String[] { null, "4", "5", "6" });

		addToDelete(new DataDeletable(participant1.getParticipantId(), model1.getId().toString()));
		participantDataDAO.append(new ParticipantDataId(participant1.getParticipantId()), model1.getId().toString(), rows1.rows,
				rows1.columns);

		addToDelete(new DataDeletable(participant2.getParticipantId(), model2.getId().toString()));
		participantDataDAO.append(new ParticipantDataId(participant2.getParticipantId()), model2.getId().toString(), rows1.rows,
				rows1.columns);

		addToDelete(new DataDeletable(participant3.getParticipantId(), model2.getId().toString()));
		participantDataDAO.append(new ParticipantDataId(participant3.getParticipantId()), model2.getId().toString(), rows1.rows,
				rows1.columns);

		Map<ParticipantDataId, ParticipantDataDescriptor> foundParticipantDatas;

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDataDescriptorsForUser(Lists.newArrayList(new ParticipantDataId(
				participant1.getParticipantId()), new ParticipantDataId(participant2.getParticipantId())));
		assertEquals(2, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDataDescriptorsForUser(Lists.newArrayList(
				new ParticipantDataId(-1), new ParticipantDataId(participant1.getParticipantId()),
				new ParticipantDataId(participant2.getParticipantId()), new ParticipantDataId(-2)));
		assertEquals(2, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDataDescriptorsForUser(Lists.newArrayList(
				new ParticipantDataId(-1), new ParticipantDataId(-2)));
		assertEquals(0, foundParticipantDatas.size());

		foundParticipantDatas = participantDataDescriptorDAO.getParticipantDataDescriptorsForUser(Lists.newArrayList(new ParticipantDataId(
				participant1.getParticipantId()), new ParticipantDataId(participant2.getParticipantId()),
				new ParticipantDataId(participant3.getParticipantId())));
		assertEquals(3, foundParticipantDatas.size());
	}

	@Test
	public void testGetSetTypes() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		List<ParticipantDataColumnDescriptor> columns = Lists.newArrayList();
		for (ParticipantDataColumnType type : ParticipantDataColumnType.values()) {
			ParticipantDataColumnDescriptor result = new ParticipantDataColumnDescriptor();
			result.setColumnType(type);
			result.setName(type.toString());
			columns.add(result);
		}

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		Date now = new Date();
		ImmutableMap.Builder<String, ParticipantDataValue> builder = ImmutableMap.builder();

		ParticipantDataStringValue stringValue = new ParticipantDataStringValue();
		stringValue.setValue("a");
		builder.put("STRING", stringValue);

		ParticipantDataBooleanValue booleanValue = new ParticipantDataBooleanValue();
		booleanValue.setValue(true);
		builder.put("BOOLEAN", booleanValue);

		ParticipantDataLongValue longValue = new ParticipantDataLongValue();
		longValue.setValue(12L);
		builder.put("LONG", longValue);

		ParticipantDataDoubleValue doubleValue = new ParticipantDataDoubleValue();
		doubleValue.setValue(3.6);
		builder.put("DOUBLE", doubleValue);

		ParticipantDataDatetimeValue datetimeValue = new ParticipantDataDatetimeValue();
		datetimeValue.setValue(now.getTime());
		builder.put("DATETIME", datetimeValue);

		ParticipantDataLabValue labValue = new ParticipantDataLabValue();
		labValue.setEnteredValue("100.00");
		labValue.setUnits("flumps");
		labValue.setNormalizedMax(11.9);
		labValue.setNormalizedMin(2.3);
		labValue.setNormalizedValue(4.8);
		builder.put("LAB", labValue);

		ParticipantDataRow row = new ParticipantDataRow();
		row.setData(builder.build());
		ParticipantDataRow nullRow = new ParticipantDataRow();
		nullRow.setData(Collections.<String, ParticipantDataValue> emptyMap());
		List<ParticipantDataRow> rows = Lists.newArrayList(row, nullRow);

		participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), rows, columns);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), columns);

		assertEquals(2, newRowSet.size());
		assertEquals(columns.size(), newRowSet.get(0).getData().size());
		assertEquals(stringValue, newRowSet.get(0).getData().get("STRING"));
		assertEquals(booleanValue, newRowSet.get(0).getData().get("BOOLEAN"));
		assertEquals(longValue, newRowSet.get(0).getData().get("LONG"));
		assertEquals(doubleValue, newRowSet.get(0).getData().get("DOUBLE"));
		assertEquals(datetimeValue, newRowSet.get(0).getData().get("DATETIME"));
		assertEquals(labValue, newRowSet.get(0).getData().get("LAB"));

		assertNull(newRowSet.get(1).getData().get("STRING"));
		assertNull(newRowSet.get(1).getData().get("BOOLEAN"));
		assertNull(newRowSet.get(1).getData().get("LONG"));
		assertNull(newRowSet.get(1).getData().get("DOUBLE"));
		assertNull(newRowSet.get(1).getData().get("DATETIME"));
		assertNull(newRowSet.get(1).getData().get("LAB"));

	}

	@Test
	public void testInvalidDeletes() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();
		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		try {
			participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), null);
			fail("Did not throw Illegal Argument Exception for null IdList");
		} catch (IllegalArgumentException e) {
		}
		try {
			IdList idList = new IdList();
			participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), idList);
			fail("Did not throw Illegal Argument Exception for IdList with no IDs");
		} catch (IllegalArgumentException e) {
		}
		try {
			IdList idList = new IdList();
			idList.setList(Lists.<Long> newArrayList());
			participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), idList);
			fail("Did not throw Illegal Argument Exception for IdList with empty idList");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testDeleteAll() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows = createRowList(new String[] { "a" }, new String[] { null, "1" }, new String[] { null, "4" }, new String[] { null, "7" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		List<ParticipantDataRow> savedRows = participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.rows, rows.columns);

		IdList idList = new IdList();
		idList.setList(Lists.<Long> newArrayList());
		for (ParticipantDataRow row : savedRows) {
			idList.getList().add(row.getRowId());
		}

		// Retain one in the middle
		participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), idList);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.columns);

		assertEquals(0, newRowSet.size());
	}

	@Test
	public void testDeleteAllButRowInMiddle() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows = createRowList(new String[] { "a" }, new String[] { null, "1" }, new String[] { null, "4" }, new String[] { null, "7" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		List<ParticipantDataRow> savedRows = participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.rows, rows.columns);

		List<Long> rowIds = Lists.newArrayListWithCapacity(savedRows.size());
		for (ParticipantDataRow row : savedRows) {
			rowIds.add(row.getRowId());
		}

		// Retain one in the middle
		rowIds.remove(1);
		IdList idList = new IdList();
		idList.setList(rowIds);
		participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), idList);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.columns);

		assertEquals(1, newRowSet.size());
		assertEquals("4", ((ParticipantDataStringValue) newRowSet.get(0).getData().get("a")).getValue());
	}

	@Test
	public void testDeleteOneRowInMiddle() throws Exception {
		DBOParticipant participant = createParticipant();
		DBOParticipantDataDescriptor model = createModel();

		Rows rows = createRowList(new String[] { "a" }, new String[] { null, "1" }, new String[] { null, "4" }, new String[] { null, "7" });

		addToDelete(new DataDeletable(participant.getParticipantId(), model.getId().toString()));

		List<ParticipantDataRow> savedRows = participantDataDAO.append(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.rows, rows.columns);

		List<Long> rowIds = Lists.newArrayListWithCapacity(savedRows.size());
		for (ParticipantDataRow row : savedRows) {
			rowIds.add(row.getRowId());
		}

		// Retain one in the middle
		rowIds.remove(2);
		rowIds.remove(0);
		IdList idList = new IdList();
		idList.setList(rowIds);
		participantDataDAO.deleteRows(new ParticipantDataId(participant.getParticipantId()), model.getId().toString(), idList);

		List<ParticipantDataRow> newRowSet = participantDataDAO.get(new ParticipantDataId(participant.getParticipantId()), model.getId()
				.toString(), rows.columns);

		assertEquals(2, newRowSet.size());
		assertEquals("1", ((ParticipantDataStringValue) newRowSet.get(0).getData().get("a")).getValue());
		assertEquals("7", ((ParticipantDataStringValue) newRowSet.get(1).getData().get("a")).getValue());
	}

}
