package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:grid-db-test-context.xml" })
public class GridIndexDaoImplTest {

	@Autowired
	private GridIndexDao gridIndexDao;

	private String sessionIdOne;
	private Long replicaIdOne;

	private String sessionIdTwo;
	private Long replicaIdTwo;

	private List<LogicalTimestamp> ids;
	private Long limit;
	private Long offset;

	@BeforeEach
	public void before() {
		gridIndexDao.truncateAll();
		sessionIdOne = GridUtils.gridSessionIdAsString(99L);
		replicaIdOne = 28L;
		sessionIdTwo = GridUtils.gridSessionIdAsString(101L);
		replicaIdTwo = 29L;

		ids = LogicalTimestampTestHelper.createIds(10);

		limit = 100L;
		offset = 0L;
	}

	@AfterEach
	public void after() {
		gridIndexDao.truncateAll();
	}

	@Test
	public void testCreateReplica() {

		Optional<Timestamp> createdOn = gridIndexDao.getReplicaCreatedOn(sessionIdOne, replicaIdOne);
		assertEquals(Optional.empty(), createdOn);

		// call under test
		assertTrue(gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne));
		assertTrue(gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne + 1L));
		assertFalse(gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne));
		assertFalse(gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne + 1L));

		createdOn = gridIndexDao.getReplicaCreatedOn(sessionIdOne, replicaIdOne);
		assertNotNull(createdOn);
		assertTrue(createdOn.isPresent());

		createdOn = gridIndexDao.getReplicaCreatedOn(sessionIdOne, replicaIdOne + 1);
		assertNotNull(createdOn);
		assertTrue(createdOn.isPresent());

		// call under test
		gridIndexDao.deleteReplica(sessionIdOne, replicaIdOne);

		createdOn = gridIndexDao.getReplicaCreatedOn(sessionIdOne, replicaIdOne);
		assertEquals(Optional.empty(), createdOn);

		createdOn = gridIndexDao.getReplicaCreatedOn(sessionIdOne, replicaIdOne + 1);
		assertNotNull(createdOn);
		assertTrue(createdOn.isPresent());

	}

	@ParameterizedTest
	@EnumSource(value = IndexType.class)
	public void testSaveIndex(IndexType type) {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);
		List<LogicalTimestamp> ids = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));

		List<LogicalTimestamp> ids2 = List.of(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));

		// call under test
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, type, ids);
		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, type, ids2);

		// call under test
		List<IndexNode> results = gridIndexDao.getIndices(sessionIdOne, replicaIdOne, ids);
		assertNotNull(results);
		List<IndexNode> expected = List.of(new IndexNode().setId(ids.get(0)).setType(type),
				new IndexNode().setId(ids.get(1)).setType(type));
		assertEquals(expected, results);

		// call under test
		results = gridIndexDao.getIndices(sessionIdTwo, replicaIdTwo, ids2);
		assertNotNull(results);
		expected = List.of(new IndexNode().setId(ids2.get(0)).setType(type),
				new IndexNode().setId(ids2.get(1)).setType(type));
		assertEquals(expected, results);
	}

	@Test
	public void testSaveIndexWithNullSessionId() {
		sessionIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, ids);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testSaveIndexWithNullRelicaId() {
		replicaIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, ids);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testSaveIndexWithType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, null, ids);
		}).getMessage();
		assertEquals("type is required.", message);
	}

	@Test
	public void testSaveIndexWithNullIds() {
		// call under test
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, null);
	}

	@Test
	public void testSaveIndexWithNullEmptyIds() {
		// call under test
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, Collections.emptyList());
	}

	@Test
	public void testGetIndeciesWithNullSessionId() {
		sessionIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getIndices(sessionIdOne, replicaIdOne, ids);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testGetIndeciesWithNullReplicaId() {
		replicaIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getIndices(sessionIdOne, replicaIdOne, ids);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testGetIndeciesWithNullBatch() {
		ids = null;
		// call under test
		assertEquals(Collections.emptyList(), gridIndexDao.getIndices(sessionIdOne, replicaIdOne, ids));
	}

	@Test
	public void testGetIndeciesWithEmptyBatch() {
		ids = Collections.emptyList();
		// call under test
		assertEquals(Collections.emptyList(), gridIndexDao.getIndices(sessionIdOne, replicaIdOne, ids));
	}

	@Test
	public void testSaveAndGetConstantEachObjectType() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);

		List<ConstantNode> constants = List.of(new ConstantNode().setId(ids.get(0)).setValue(true),
				new ConstantNode().setId(ids.get(1)).setValue(101),
				new ConstantNode().setId(ids.get(2)).setValue(505555555555555555L),
				new ConstantNode().setId(ids.get(3)).setValue(3.14),
				new ConstantNode().setId(ids.get(4)).setValue("Hello World"),
				new ConstantNode().setId(ids.get(5)).setValue(new JSONArray("[1,2,3]")),
				new ConstantNode().setId(ids.get(6)).setValue(new JSONObject("{\"key\":99}")),
				new ConstantNode().setId(ids.get(7)).setValue(JSONObject.NULL),
				new ConstantNode().setId(ids.get(8)).setValue(null));

		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, ids);
		// call under test
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants);
		// call under test
		List<ConstantNode> results = gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		assertEquals(constants, results);
	}

	@Test
	public void testSaveAndGetConstantWithDuplicate() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);

		List<ConstantNode> constants = List.of(new ConstantNode().setId(ids.get(0)).setValue(101),
				new ConstantNode().setId(ids.get(0)).setValue(102));

		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, ids);
		// call under test
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants);
		// call under test
		List<ConstantNode> results = gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		assertEquals(constants.subList(0, 1), results);
	}

	@Test
	public void testSaveAndGetConstantWithMultipleSessionIds() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		List<ConstantNode> constants1 = List.of(new ConstantNode().setId(ids.get(0)).setValue("Hello from session 1"));
		List<ConstantNode> constants2 = List.of(new ConstantNode().setId(ids.get(1)).setValue("Hello from session 2"));

		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, ids);
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants1);

		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, IndexType.con, ids);
		gridIndexDao.saveNewConstants(sessionIdTwo, replicaIdTwo, constants2);

		List<ConstantNode> results1 = gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		assertEquals(constants1, results1);

		List<ConstantNode> results2 = gridIndexDao.getConstants(sessionIdTwo, replicaIdTwo, ids);
		assertEquals(constants2, results2);
	}

	@Test
	public void testSaveNewConstantsWithNullSessionId() {
		List<ConstantNode> constants1 = List.of(new ConstantNode().setId(ids.get(0)).setValue("Hello from session 1"));
		sessionIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants1);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testSaveNewConstantsWithNullReplicaIdd() {
		List<ConstantNode> constants1 = List.of(new ConstantNode().setId(ids.get(0)).setValue("Hello from session 1"));
		replicaIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants1);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testSaveNewConstantsWithNullConstants() {
		List<ConstantNode> constants1 = null;
		// call under test
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants1);
	}

	@Test
	public void testSaveNewConstantsWithEmptyConstants() {
		List<ConstantNode> constants1 = Collections.emptyList();
		// call under test
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, constants1);
	}
	
	@Test
	public void testGetConstantsWithIdsHasNull() {
		ids = new ArrayList<>();
		ids.add(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		ids.add(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		}).getMessage();
		assertEquals("ids list cannot contain null values", message);
	}

	@Test
	public void testGetConstantsWithNullSessionId() {
		sessionIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testGetConstantsWithNullReplicaId() {
		replicaIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testGetConstantsWithNullIds() {
		ids = null;
		// call under test
		assertEquals(Collections.emptyList(), gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids));
	}

	@Test
	public void testGetConstantsWithEmptyIds() {
		ids = Collections.emptyList();
		// call under test
		assertEquals(Collections.emptyList(), gridIndexDao.getConstants(sessionIdOne, replicaIdOne, ids));
	}

	@Test
	public void testGetAndSetClock() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		LogicalTimestamp clockOne = ids.get(0);
		LogicalTimestamp clockTwo = ids.get(1);
		LogicalTimestamp clockThree = ids.get(2);

		// call under test
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, clockOne);
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, clockThree);
		// call under test
		gridIndexDao.setClock(sessionIdTwo, replicaIdTwo, clockTwo);
		gridIndexDao.setClock(sessionIdTwo, replicaIdTwo, clockThree);

		// call under test
		assertEquals(Optional.of(clockOne.getSequenceNumber()),
				gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdOne, clockOne.getReplicaId()));
		assertEquals(Optional.of(clockThree.getSequenceNumber()),
				gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdOne, clockThree.getReplicaId()));

		// call under test
		assertEquals(Optional.of(clockTwo.getSequenceNumber()),
				gridIndexDao.getClockSequenceNumber(sessionIdTwo, replicaIdTwo, clockTwo.getReplicaId()));
		assertEquals(Optional.of(clockThree.getSequenceNumber()),
				gridIndexDao.getClockSequenceNumber(sessionIdTwo, replicaIdTwo, clockThree.getReplicaId()));

		// call under test
		assertEquals(List.of(clockOne, clockThree), gridIndexDao.getClock(sessionIdOne, replicaIdOne));
		assertEquals(List.of(clockTwo, clockThree), gridIndexDao.getClock(sessionIdTwo, replicaIdTwo));
	}

	@Test
	public void testGetAndSetClockWithUpdate() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);

		LogicalTimestamp start = new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(11L);
		LogicalTimestamp updated = new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(12L);

		// call under test
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, start);
		assertEquals(Optional.of(11L), gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdOne, 9L));
		assertEquals(List.of(start), gridIndexDao.getClock(sessionIdOne, replicaIdOne));
		// update
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, updated);
		assertEquals(Optional.of(12L), gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdOne, 9L));
		assertEquals(List.of(updated), gridIndexDao.getClock(sessionIdOne, replicaIdOne));

	}

	@Test
	public void testGetClockWithDoesNotExist() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);

		LogicalTimestamp clockOne = ids.get(0);

		// call under test
		assertEquals(Optional.empty(),
				gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdOne, clockOne.getReplicaId()));

	}

	@Test
	public void testSetClockWithNullSessionId() {
		sessionIdOne = null;
		LogicalTimestamp clock = ids.get(0);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.setClock(sessionIdOne, replicaIdOne, clock);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testSetClockWithNullReplicaId() {
		replicaIdOne = null;
		LogicalTimestamp clock = ids.get(0);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.setClock(sessionIdOne, replicaIdOne, clock);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testSetClockWithNullClock() {
		LogicalTimestamp clock = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.setClock(sessionIdOne, replicaIdOne, clock);
		}).getMessage();
		assertEquals("clock is required.", message);
	}

	@Test
	public void testGetClockWithNullSessionId() {
		sessionIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getClock(sessionIdOne, replicaIdOne);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testGetClockWithNullReplicaId() {
		replicaIdOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getClock(sessionIdOne, replicaIdOne);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testGetClockSequenceNumberWithNullSessionId() {
		sessionIdOne = null;
		Long clockId = 88L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdTwo, clockId);
		}).getMessage();
		assertEquals("sessionId is required.", message);
	}

	@Test
	public void testGetClockSequenceNumberWithNullReplicaId() {
		replicaIdTwo = null;
		Long clockId = 88L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdTwo, clockId);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testGetClockSequenceNumberWithNullClock() {
		Long clockId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridIndexDao.getClockSequenceNumber(sessionIdOne, replicaIdTwo, clockId);
		}).getMessage();
		assertEquals("clockIdRep is required.", message);
	}

	@Test
	public void testSaveAndGetObjects() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		Map<String, LogicalTimestamp> value = new LinkedHashMap<>();
		value.put("one", ids.get(2));
		value.put("two", ids.get(3));
		List<ObjectNode> objectsOne = List.of(new ObjectNode().setId(ids.get(0)),
				new ObjectNode().setId(ids.get(1)).setValue(value));
		List<ObjectNode> objectsTwo = List.of(new ObjectNode().setId(ids.get(2)),
				new ObjectNode().setId(ids.get(3)).setValue(value));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.obj,
				objectsOne.stream().map(ObjectNode::getId).collect(Collectors.toList()));
		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, IndexType.obj,
				objectsTwo.stream().map(ObjectNode::getId).collect(Collectors.toList()));
		// all under test
		gridIndexDao.saveObjects(sessionIdOne, replicaIdOne, objectsOne);
		gridIndexDao.saveObjects(sessionIdTwo, replicaIdTwo, objectsTwo);
		// call under test
		assertEquals(objectsOne, gridIndexDao.getObjects(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(1))));
		assertEquals(objectsTwo, gridIndexDao.getObjects(sessionIdTwo, replicaIdTwo, List.of(ids.get(2), ids.get(3))));

		ObjectNode updated = new ObjectNode().setId(ids.get(0)).setValueFromJson("{\"a\":[7,8],\"b\":[9,10]}");
		// update the first object
		gridIndexDao.saveObjects(sessionIdOne, replicaIdOne, List.of(updated));
		assertEquals(List.of(updated, objectsOne.get(1)),
				gridIndexDao.getObjects(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(1))));
		assertEquals(objectsTwo, gridIndexDao.getObjects(sessionIdTwo, replicaIdTwo, List.of(ids.get(2), ids.get(3))));
	}

	@Test
	public void testSaveAndGetValues() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		List<ValueNode> valuesOne = List.of(new ValueNode().setId(ids.get(0)).setValue(ids.get(1)),
				new ValueNode().setId(ids.get(2)).setValue(ids.get(3)));
		List<ValueNode> valuesTwo = List.of(new ValueNode().setId(ids.get(4)).setValue(ids.get(5)),
				new ValueNode().setId(ids.get(6)).setValue(ids.get(7)));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.val,
				valuesOne.stream().map(ValueNode::getId).collect(Collectors.toList()));
		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, IndexType.val,
				valuesTwo.stream().map(ValueNode::getId).collect(Collectors.toList()));
		// all under test
		gridIndexDao.saveValues(sessionIdOne, replicaIdOne, valuesOne);
		gridIndexDao.saveValues(sessionIdTwo, replicaIdTwo, valuesTwo);
		// call under test
		assertEquals(valuesOne, gridIndexDao.getValues(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(2))));
		assertEquals(valuesTwo, gridIndexDao.getValues(sessionIdTwo, replicaIdTwo, List.of(ids.get(4), ids.get(6))));

		ValueNode updated = new ValueNode().setId(ids.get(0)).setValueFromJson("[7,8]");
		// update the first object
		gridIndexDao.saveValues(sessionIdOne, replicaIdOne, List.of(updated));
		assertEquals(List.of(updated, valuesOne.get(1)),
				gridIndexDao.getValues(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(2))));
		assertEquals(valuesTwo, gridIndexDao.getValues(sessionIdTwo, replicaIdTwo, List.of(ids.get(4), ids.get(6))));
	}

	@Test
	public void testSaveAndGetVectors() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		List<VectorNode> valuesOne = List.of(
				new VectorNode().setId(ids.get(0))
						.setValueFromJson("{\"c0\":{\"v\":[123],\"i\":[3,4]},\"c1\":{\"v\":[\"one\"],\"i\":[5,6]}}"),
				new VectorNode().setId(ids.get(1))
						.setValueFromJson("{\"c0\":{\"v\":[456],\"i\":[7,8]},\"c1\":{\"v\":[\"two\"],\"i\":[9,10]}}"));
		List<VectorNode> valuesTwo = List.of(
				new VectorNode().setId(ids.get(2))
						.setValueFromJson("{\"c0\":{\"v\":[111],\"i\":[11,12]},\"c1\":{\"v\":[\"one\"],\"i\":[13,14]}}"),
				new VectorNode().setId(ids.get(3))
						.setValueFromJson("{\"c0\":{\"v\":[222],\"i\":[15,16]},\"c1\":{\"v\":[\"two\"],\"i\":[17,18]}}"));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.vec,
				valuesOne.stream().map(VectorNode::getId).collect(Collectors.toList()));
		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, IndexType.vec,
				valuesTwo.stream().map(VectorNode::getId).collect(Collectors.toList()));
		// all under test
		gridIndexDao.saveVectors(sessionIdOne, replicaIdOne, valuesOne);
		gridIndexDao.saveVectors(sessionIdTwo, replicaIdTwo, valuesTwo);
		// call under test
		assertEquals(valuesOne, gridIndexDao.getVectors(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(1))));
		assertEquals(valuesTwo, gridIndexDao.getVectors(sessionIdTwo, replicaIdTwo, List.of(ids.get(2), ids.get(3))));

		VectorNode updated = new VectorNode().setId(ids.get(0))
				.setValueFromJson("{\"c0\":{\"v\":[888],\"i\":[3,4]},\"c1\":{\"v\":[\"three\"],\"i\":[5,6]}}");
		// update the first object
		gridIndexDao.saveVectors(sessionIdOne, replicaIdOne, List.of(updated));
		assertEquals(List.of(updated, valuesOne.get(1)),
				gridIndexDao.getVectors(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(1))));
		assertEquals(valuesTwo, gridIndexDao.getVectors(sessionIdTwo, replicaIdTwo, List.of(ids.get(2), ids.get(3))));
	}

	@Test
	public void testSaveAndGetArrays() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);

		LogicalTimestamp arrOneId = new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(44L);
		LogicalTimestamp arrTwoId = new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(44L);

		List<RGANode> valuesOne = List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(0)).setDataId(ids.get(1))
						.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(2)).setDataId(ids.get(3))
						.setReferenceNodeId(ids.get(0)).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(5))
						.setReferenceNodeId(ids.get(2)).setIsDeleted(false));

		List<RGANode> valuesTwo = List.of(
				new RGANode().setContainerId(arrTwoId).setNodeId(ids.get(0)).setDataId(ids.get(1))
						.setReferenceNodeId(arrTwoId).setIsDeleted(false),
				new RGANode().setContainerId(arrTwoId).setNodeId(ids.get(2)).setDataId(ids.get(3))
						.setReferenceNodeId(ids.get(0)).setIsDeleted(false),
				new RGANode().setContainerId(arrTwoId).setNodeId(ids.get(4)).setDataId(ids.get(5))
						.setReferenceNodeId(ids.get(2)).setIsDeleted(false));

		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.arr, List.of(arrOneId));
		gridIndexDao.saveIndex(sessionIdTwo, replicaIdTwo, IndexType.arr, List.of(arrTwoId));
		// call under test
		gridIndexDao.createArrayBatch(sessionIdOne, replicaIdOne, List.of(arrOneId));
		gridIndexDao.createArrayBatch(sessionIdTwo, replicaIdTwo, List.of(arrTwoId));

		valuesOne.forEach(a -> {
			// this insert is not a conflict so it should be inserted at it starting
			// location
			assertEquals(Optional.of(a.getReferenceNodeId()),
					gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, a));
			// call under test
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, a);
		});
		valuesTwo.forEach(a -> {
			// this insert is not a conflict so it should be inserted at it starting
			// location
			assertEquals(Optional.of(a.getReferenceNodeId()),
					gridIndexDao.findRgaInsertLocation(sessionIdTwo, replicaIdTwo, a));
			// call under test
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdTwo, replicaIdTwo, a);
		});

		// call under test
		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(valuesOne.get(0), valuesOne.get(1), valuesOne.get(2))),
				gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset));
		assertEquals(new ArrayNode().setId(arrTwoId).setElements(List.of(valuesTwo.get(0), valuesTwo.get(1), valuesTwo.get(2))),
				gridIndexDao.getArrayNode(sessionIdTwo, replicaIdTwo, arrTwoId, false, limit, offset));

		// insert a value between 0 and 1
		RGANode toInsert = new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(7))
				.setReferenceNodeId(valuesOne.get(0).getNodeId()).setIsDeleted(false);
		gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, toInsert);
		// the reference of the old node should now point to the new node.
		valuesOne.get(1).setReferenceNodeId(toInsert.getNodeId());

		// call under test
		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(valuesOne.get(0), toInsert, valuesOne.get(1), valuesOne.get(2))),
				gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset));
		assertEquals(new ArrayNode().setId(arrTwoId).setElements(List.of(valuesTwo.get(0), valuesTwo.get(1), valuesTwo.get(2))),
				gridIndexDao.getArrayNode(sessionIdTwo, replicaIdTwo, arrTwoId, false, limit, offset));

		// attempt to insert an early data id at the beginning of the array
		RGANode nextInsert = new RGANode().setContainerId(arrOneId).setNodeId(ids.get(8)).setDataId(ids.get(0))
				.setReferenceNodeId(arrOneId);
		// call under test
		assertEquals(Optional.of(ids.get(4)),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, nextInsert));

	}

	@Test
	public void testFindRgaInsertLocation() {
		LogicalTimestamp arrOneId = new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(44L);
		createArray(sessionIdOne, replicaIdOne, arrOneId);

		List<RGANode> valuesOne = List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
						.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(5)).setDataId(ids.get(3))
						.setReferenceNodeId(ids.get(4)).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
						.setReferenceNodeId(ids.get(5)).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(7)).setDataId(ids.get(2))
						.setReferenceNodeId(ids.get(6)).setIsDeleted(false));

		valuesOne.forEach(a -> {
			assertEquals(Optional.of(a.getReferenceNodeId()),
					gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, a));
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, a);
		});

		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(valuesOne.get(0), valuesOne.get(1), valuesOne.get(2), valuesOne.get(3))),
				gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset));

		/*
		 * Call under test. Insert a node that has a unique ID but also has the same
		 * data value at the insert position. For such a case an empty result indicates
		 * that the inserted node would be a duplicates and should not be inserted.
		 */
		assertEquals(Optional.empty(), gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, new RGANode()
				.setContainerId(arrOneId).setNodeId(ids.get(8)).setDataId(ids.get(1)).setReferenceNodeId(arrOneId)));

		/*
		 * Call under test. Insert a node after the third node that has a data value
		 * larger than the data value of the node already at that position. For this
		 * cases the new node should be inserted at that exact location.
		 */
		assertEquals(Optional.of(valuesOne.get(2).getNodeId()),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, new RGANode().setContainerId(arrOneId)
						.setNodeId(ids.get(8)).setDataId(ids.get(9)).setReferenceNodeId(valuesOne.get(2).getNodeId())));

		/*
		 * Call under test. Insert a node with a data value that is smaller than all
		 * other nodes in the RGA at the beginning of the array. For this case the new
		 * node should reference the last node in the RGA (appended to the end of the
		 * array).
		 */
		assertEquals(Optional.of(valuesOne.get(3).getNodeId()),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne,
						new RGANode().setContainerId(arrOneId).setNodeId(ids.get(8))
								.setDataId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
								.setReferenceNodeId(arrOneId)));

		/*
		 * Call under test. Insert a node at the start of the array with a data value
		 * less than the first two nodes in the RGA. The node should be inserted after
		 * the second node.
		 */
		assertEquals(Optional.of(valuesOne.get(1).getNodeId()),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne,
						new RGANode().setContainerId(arrOneId).setNodeId(ids.get(8))
								.setDataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L))
								.setReferenceNodeId(arrOneId)));

		/*
		 * Call under test. Same as the previous test but with the first node as a
		 * reference, should produce the same results as the previous test.
		 */
		assertEquals(Optional.of(valuesOne.get(1).getNodeId()),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne,
						new RGANode().setContainerId(arrOneId).setNodeId(ids.get(8))
								.setDataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L))
								.setReferenceNodeId(valuesOne.get(1).getNodeId())));

		/*
		 * Call under test. Inserting a node that is already in the RGA should return
		 * Optional.empty()
		 */
		assertEquals(Optional.empty(),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, valuesOne.get(0)));
		/*
		 * Call under test. Inserting a node that is already in the RGA should return
		 * Optional.empty()
		 */
		assertEquals(Optional.empty(),
				gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, valuesOne.get(2)));

	}

	@Test
	public void testCreateNextMessageId() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);
		int maxValues = 3;
		assertEquals(0, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));
		assertEquals(0, gridIndexDao.createNextMessageId(sessionIdTwo, replicaIdTwo, maxValues));

		assertEquals(1, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));
		assertEquals(2, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));
		assertEquals(3, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));
		assertEquals(0, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));
		assertEquals(1, gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues));

		assertEquals(1, gridIndexDao.createNextMessageId(sessionIdTwo, replicaIdTwo, maxValues));

	}

	@Test
	public void testMessageChainCRUD() throws InterruptedException {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);
		int maxValues = 100;
		Duration expires = Duration.ofSeconds(2);
		// one
		Integer idOne = gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues);
		assertEquals(Optional.empty(), gridIndexDao.getMessageChain(sessionIdOne, replicaIdOne, idOne));
		MessageChain chainOne = new MessageChain().setSessionId(sessionIdOne).setReplicaId(replicaIdOne).setId(idOne)
				.setMethod("methodOne");
		MessageChain backOne = gridIndexDao.createMessageChain(chainOne, expires);
		MessageChain expected = new MessageChain().setSessionId(sessionIdOne).setReplicaId(replicaIdOne).setId(idOne)
				.setMethod("methodOne").setCreatedOn(backOne.getCreatedOn());
		assertEquals(expected, backOne);
		assertEquals(Optional.of(expected), gridIndexDao.getMessageChain(sessionIdOne, replicaIdOne, idOne));
		assertEquals(Optional.of(expected),
				gridIndexDao.getNonExpiredMessageChain(sessionIdOne, replicaIdOne, chainOne.getMethod()));
		Thread.sleep(2001L);
		assertEquals(Optional.empty(),
				gridIndexDao.getNonExpiredMessageChain(sessionIdOne, replicaIdOne, chainOne.getMethod()));
		
		// update
		chainOne.setMethod("updatedMethod");
		backOne = gridIndexDao.createMessageChain(chainOne, expires);
		expected = new MessageChain().setSessionId(sessionIdOne).setReplicaId(replicaIdOne).setId(idOne)
				.setMethod("updatedMethod").setCreatedOn(backOne.getCreatedOn());
		assertEquals(expected, backOne);

		// two
		Integer idTwo = gridIndexDao.createNextMessageId(sessionIdTwo, replicaIdTwo, maxValues);
		MessageChain chainTwo = new MessageChain().setSessionId(sessionIdTwo).setReplicaId(replicaIdTwo).setId(idTwo)
				.setMethod("methodTwo");
		backOne = gridIndexDao.createMessageChain(chainTwo, expires);
		expected = new MessageChain().setSessionId(sessionIdTwo).setReplicaId(replicaIdTwo).setId(idTwo)
				.setMethod("methodTwo").setCreatedOn(backOne.getCreatedOn());
		assertEquals(expected, backOne);
		assertEquals(Optional.of(expected), gridIndexDao.getMessageChain(sessionIdTwo, replicaIdTwo, idTwo));

		// call under test
		gridIndexDao.deleteMessageChain(sessionIdOne, replicaIdOne, idOne);
		assertEquals(Optional.empty(), gridIndexDao.getMessageChain(sessionIdOne, replicaIdOne, idOne));
		assertEquals(Optional.of(expected), gridIndexDao.getMessageChain(sessionIdTwo, replicaIdTwo, idTwo));
	}
	
	@Test
	public void testMessageChainExpiration() throws InterruptedException {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		int maxValues = 100;
		Duration expires = Duration.ofSeconds(1);
		// one
		Integer idOne = gridIndexDao.createNextMessageId(sessionIdOne, replicaIdOne, maxValues);
		assertEquals(Optional.empty(), gridIndexDao.getMessageChain(sessionIdOne, replicaIdOne, idOne));
		MessageChain chainOne = new MessageChain().setSessionId(sessionIdOne).setReplicaId(replicaIdOne).setId(idOne)
				.setMethod("methodOne");
		// call under test
		MessageChain backOne = gridIndexDao.createMessageChain(chainOne, expires);
		Thread.sleep(1000L);
		expires = Duration.ofSeconds(10);
		// call under test
		assertTrue(gridIndexDao.refreshMessageChain(sessionIdOne, replicaIdOne, idOne, expires));
		Thread.sleep(1000L);
		// all under test
		assertEquals(Optional.of(backOne),
				gridIndexDao.getNonExpiredMessageChain(sessionIdOne, replicaIdOne, chainOne.getMethod()));
	}

	@Test
	public void testGetRootObject() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		ConstantNode con = new ConstantNode().setId(ids.get(0)).setValue(new ConValue(ConType.LONG, 123L));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, List.of(con.getId()));
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, List.of(con));
		ObjectNode rootObj = new ObjectNode().setId(ids.get(1)).setValue(Map.of("aCon", con.getId()));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.obj, List.of(rootObj.getId()));
		gridIndexDao.saveObjects(sessionIdOne, replicaIdOne, List.of(rootObj));
		ValueNode root = new ValueNode().setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValue(rootObj.getId());
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.val, List.of(root.getId()));
		gridIndexDao.saveValues(sessionIdOne, replicaIdOne, List.of(root));

		// call under test
		assertEquals(Optional.of(rootObj), gridIndexDao.getRootObject(sessionIdOne, replicaIdOne));
	}

	@Test
	public void testGetRootObjectWithRootNoValue() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		ValueNode root = new ValueNode().setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValue(null);
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.val, List.of(root.getId()));
		gridIndexDao.saveValues(sessionIdOne, replicaIdOne, List.of(root));

		// call under test
		assertEquals(Optional.empty(), gridIndexDao.getRootObject(sessionIdOne, replicaIdOne));
	}

	@Test
	public void testGetRootObjectWithNoRoot() {
		// call under test
		assertEquals(Optional.empty(), gridIndexDao.getRootObject(sessionIdOne, replicaIdOne));
	}

	@Test
	public void testGetRootObjectWithRootNotAnObject() {
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		ConstantNode con = new ConstantNode().setId(ids.get(0)).setValue(new ConValue(ConType.LONG, 123L));
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.con, List.of(con.getId()));
		gridIndexDao.saveNewConstants(sessionIdOne, replicaIdOne, List.of(con));
		ValueNode root = new ValueNode().setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValue(con.getId());
		gridIndexDao.saveIndex(sessionIdOne, replicaIdOne, IndexType.val, List.of(root.getId()));
		gridIndexDao.saveValues(sessionIdOne, replicaIdOne, List.of(root));

		// call under test
		assertEquals(Optional.empty(), gridIndexDao.getRootObject(sessionIdOne, replicaIdOne));
	}
	
	@Test
	public void testDeleteRgaNodes() {
		LogicalTimestamp arrOneId = new LogicalTimestamp().setReplicaId(replicaIdOne).setSequenceNumber(44L);
		LogicalTimestamp arrTwoId = new LogicalTimestamp().setReplicaId(replicaIdTwo).setSequenceNumber(44L);
		
		createArray(sessionIdOne, replicaIdOne, arrOneId);
		createArray(sessionIdTwo, replicaIdTwo, arrTwoId);

		List<RGANode> valuesOne = List.of(
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
					.setReferenceNodeId(arrOneId),
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(5)).setDataId(ids.get(3))
					.setReferenceNodeId(ids.get(4)),
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
					.setReferenceNodeId(ids.get(5)),
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(7)).setDataId(ids.get(2))
					.setReferenceNodeId(ids.get(6))
		);

		valuesOne.forEach(a -> {
			assertEquals(Optional.of(a.getReferenceNodeId()),
					gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, a));
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, a);
		});
		
		List<RGANode> valuesTwo = List.of(
			new RGANode().setContainerId(arrTwoId).setNodeId(ids.get(8)).setDataId(ids.get(1))
					.setReferenceNodeId(arrTwoId).setIsDeleted(false),
			new RGANode().setContainerId(arrTwoId).setNodeId(ids.get(9)).setDataId(ids.get(3))
					.setReferenceNodeId(ids.get(8)).setIsDeleted(false)
		);
		
		valuesTwo.forEach(a -> {
			assertEquals(Optional.of(a.getReferenceNodeId()),
					gridIndexDao.findRgaInsertLocation(sessionIdTwo, replicaIdTwo, a));
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdTwo, replicaIdTwo, a);
		});
		
		List<Timespan> toDelete = valuesOne.stream().map(a -> new Timespan(a.getNodeId(), 1L)).collect(Collectors.toList());
		
		// Call under test
		gridIndexDao.deleteRgaNodes(sessionIdOne, replicaIdOne, arrOneId, toDelete);

		assertEquals(new ArrayNode().setId(arrOneId).setElements(Collections.emptyList()), gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset));
		assertEquals(new ArrayNode().setId(arrTwoId).setElements(valuesTwo), gridIndexDao.getArrayNode(sessionIdTwo, replicaIdTwo, arrTwoId, false, limit, offset));

		// includeTombstones = true should still return the nodes marked as deleted
		List<RGANode> valuesOneMarkedAsDeleted = valuesOne.stream()
			.map(a -> a.setIsDeleted(true))
			.collect(Collectors.toList());
		assertEquals(new ArrayNode().setId(arrOneId).setElements(valuesOneMarkedAsDeleted), gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, true, limit, offset));
		assertEquals(new ArrayNode().setId(arrTwoId).setElements(valuesTwo), gridIndexDao.getArrayNode(sessionIdTwo, replicaIdTwo, arrTwoId, true, limit, offset));


	}
	
	@Test
	public void testDeleteAndInsertRgaNodes() {
		LogicalTimestamp arrOneId = new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(44L);
		
		createArray(sessionIdOne, replicaIdOne, arrOneId);

		List<RGANode> valuesOne = List.of(
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
				.setReferenceNodeId(arrOneId).setIsDeleted(false),
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(5)).setDataId(ids.get(3))
				.setReferenceNodeId(ids.get(4)).setIsDeleted(false),
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
				.setReferenceNodeId(ids.get(5)).setIsDeleted(false)
		);

		valuesOne.forEach(a -> {
			assertEquals(Optional.of(a.getReferenceNodeId()), gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, a));
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, a);
		});

		assertEquals(new ArrayNode().setId(arrOneId).setElements(valuesOne), gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset));
		
		// Call under test, deletes a node in the middle of the array
		gridIndexDao.deleteRgaNodes(sessionIdOne, replicaIdOne, arrOneId, List.of(
			new Timespan(ids.get(5), 1L)
		));

		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
					.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
					.setReferenceNodeId(ids.get(5)).setIsDeleted(false)
			)),
			gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset)
		);
		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
						.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(5)).setDataId(ids.get(3))
						.setReferenceNodeId(ids.get(4)).setIsDeleted(true),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
						.setReferenceNodeId(ids.get(5)).setIsDeleted(false)
			)),
			gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, true, limit, offset)
		);

		// Now insert an additional node after 4
		valuesOne = List.of(
			new RGANode().setContainerId(arrOneId).setNodeId(ids.get(7)).setDataId(ids.get(8))
				.setReferenceNodeId(ids.get(4)).setIsDeleted(false)
		);

		valuesOne.forEach(a -> {
			assertEquals(Optional.of(a.getReferenceNodeId()), gridIndexDao.findRgaInsertLocation(sessionIdOne, replicaIdOne, a));
			gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, a);
		});
		
		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
					.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(7)).setDataId(ids.get(8))
					.setReferenceNodeId(ids.get(4)).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
					.setReferenceNodeId(ids.get(5)).setIsDeleted(false)
			)),
			gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, false, limit, offset)
		);
		assertEquals(new ArrayNode().setId(arrOneId).setElements(List.of(
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(4)).setDataId(ids.get(1))
					.setReferenceNodeId(arrOneId).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(7)).setDataId(ids.get(8))
					.setReferenceNodeId(ids.get(4)).setIsDeleted(false),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(5)).setDataId(ids.get(3))
					// NOTE: the deleted node now references the new node
					.setReferenceNodeId(ids.get(7)).setIsDeleted(true),
				new RGANode().setContainerId(arrOneId).setNodeId(ids.get(6)).setDataId(ids.get(0))
					.setReferenceNodeId(ids.get(5)).setIsDeleted(false)
			)),
			gridIndexDao.getArrayNode(sessionIdOne, replicaIdOne, arrOneId, true, limit, offset)
		);


	}
	
	@Test
	public void testGetRgaLastNode() {
		// Creates an empty array
		LogicalTimestamp arrOneId = new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(44L);
		
		createArray(sessionIdOne, replicaIdOne, arrOneId);
		
		// Call under test
		Optional<RGANode> lastNode = gridIndexDao.getRgaLastNode(sessionIdOne, replicaIdOne, arrOneId);
		
		assertTrue(lastNode.isEmpty());
		
		RGANode firstNode = new RGANode()
			.setContainerId(arrOneId)
			.setNodeId(ids.get(4))
			.setDataId(ids.get(1))
			.setReferenceNodeId(arrOneId)
			.setIsDeleted(false);
		
		gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, firstNode);
		
		// Call under test
		assertEquals(Optional.of(firstNode), gridIndexDao.getRgaLastNode(sessionIdOne, replicaIdOne, arrOneId));
		
		RGANode secondNode = new RGANode()
			.setContainerId(arrOneId)
			.setNodeId(ids.get(5))
			.setDataId(ids.get(3))
			.setReferenceNodeId(firstNode.getNodeId())
			.setIsDeleted(false);
		
		gridIndexDao.insertIntoRepeatedGrowableArray(sessionIdOne, replicaIdOne, secondNode);
		
		// Call under test
		assertEquals(Optional.of(secondNode), gridIndexDao.getRgaLastNode(sessionIdOne, replicaIdOne, arrOneId));
		
		// Deletes the first node
		gridIndexDao.deleteRgaNodes(sessionIdOne, replicaIdOne, arrOneId, List.of(
			new Timespan(firstNode.getNodeId(), 1L)
		));
		
		// Call under test
		assertEquals(Optional.of(secondNode), gridIndexDao.getRgaLastNode(sessionIdOne, replicaIdOne, arrOneId));
	}
	
	@Test
	public void testGetClockSequenceMaximum() {
		// call under test
		assertEquals(1L, gridIndexDao.getClockSequenceMaximum(sessionIdOne, replicaIdOne));
		gridIndexDao.createReplicaIfNotExists(sessionIdOne, replicaIdOne);
		gridIndexDao.createReplicaIfNotExists(sessionIdTwo, replicaIdTwo);
		
		// call under test
		assertEquals(1L, gridIndexDao.getClockSequenceMaximum(sessionIdOne, replicaIdOne));
		assertEquals(1L, gridIndexDao.getClockSequenceMaximum(sessionIdTwo, replicaIdTwo));
		
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L));
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		gridIndexDao.setClock(sessionIdOne, replicaIdOne, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		
		gridIndexDao.setClock(sessionIdTwo, replicaIdTwo, new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		gridIndexDao.setClock(sessionIdTwo, replicaIdTwo, new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L));
		gridIndexDao.setClock(sessionIdTwo, replicaIdTwo, new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L));
		
		// call under test
		assertEquals(6L, gridIndexDao.getClockSequenceMaximum(sessionIdOne, replicaIdOne));
		assertEquals(12L, gridIndexDao.getClockSequenceMaximum(sessionIdTwo, replicaIdTwo));
		
	}

	/**
	 * Helper to create a new array.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param arrayId
	 */
	void createArray(String sessionId, Long replicaId, LogicalTimestamp arrayId) {
		gridIndexDao.createReplicaIfNotExists(sessionId, replicaId);
		gridIndexDao.saveIndex(sessionId, replicaId, IndexType.arr, List.of(arrayId));
		gridIndexDao.createArrayBatch(sessionId, replicaId, List.of(arrayId));
	}
}
