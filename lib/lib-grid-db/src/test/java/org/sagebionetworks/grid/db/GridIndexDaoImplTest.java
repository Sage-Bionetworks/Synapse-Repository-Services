package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
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
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
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

	@BeforeEach
	public void before() {
		gridIndexDao.truncateAll();
		sessionIdOne = GridUtils.gridSessionIdAsString(99L);
		replicaIdOne = 28L;
		sessionIdTwo = GridUtils.gridSessionIdAsString(101L);
		replicaIdTwo = 29L;

		ids = List.of(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L),
				new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(10L),
				new LogicalTimestamp().setReplicaId(11L).setSequenceNumber(12L),
				new LogicalTimestamp().setReplicaId(13L).setSequenceNumber(14L),
				new LogicalTimestamp().setReplicaId(15L).setSequenceNumber(16L));
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
				new ConstantNode().setId(ids.get(7)).setValue(null));

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
						.setValueFromJson("{\"c0\":{\"v\":123,\"i\":[3,4]},\"c1\":{\"v\":\"one\",\"i\":[5,6]}}"),
				new VectorNode().setId(ids.get(1))
						.setValueFromJson("{\"c0\":{\"v\":456,\"i\":[7,8]},\"c1\":{\"v\":\"two\",\"i\":[9,10]}}"));
		List<VectorNode> valuesTwo = List.of(
				new VectorNode().setId(ids.get(2))
						.setValueFromJson("{\"c0\":{\"v\":111,\"i\":[11,12]},\"c1\":{\"v\":\"one\",\"i\":[13,14]}}"),
				new VectorNode().setId(ids.get(3))
						.setValueFromJson("{\"c0\":{\"v\":222,\"i\":[15,16]},\"c1\":{\"v\":\"two\",\"i\":[17,18]}}"));
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
				.setValueFromJson("{\"c0\":{\"v\":888,\"i\":[3,4]},\"c1\":{\"v\":\"three\",\"i\":[5,6]}}");
		// update the first object
		gridIndexDao.saveVectors(sessionIdOne, replicaIdOne, List.of(updated));
		assertEquals(List.of(updated, valuesOne.get(1)),
				gridIndexDao.getVectors(sessionIdOne, replicaIdOne, List.of(ids.get(0), ids.get(1))));
		assertEquals(valuesTwo, gridIndexDao.getVectors(sessionIdTwo, replicaIdTwo, List.of(ids.get(2), ids.get(3))));
	}
}
