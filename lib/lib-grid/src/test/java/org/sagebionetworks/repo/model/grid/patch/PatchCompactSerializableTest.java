package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ClasspathUtil;

public class PatchCompactSerializableTest {

	private Map<OperationType, List<String>> examplePatches;

	@BeforeEach
	public void before() {

		examplePatches = new HashMap<>();
		examplePatches.put(OperationType.new_con,
				Arrays.asList("[0,\"foo\"]", "[0]", "[0,[123,456],true]", "[0,10,true]"));
		examplePatches.put(OperationType.new_val, Arrays.asList("[1]", "[1],[1]"));
		examplePatches.put(OperationType.new_obj, Arrays.asList("[2]", "[2],[2]"));
		examplePatches.put(OperationType.new_vec, Arrays.asList("[3]", "[3],[3]"));
		examplePatches.put(OperationType.new_str, Arrays.asList("[4]", "[4],[4]"));
		examplePatches.put(OperationType.new_bin, Arrays.asList("[5]", "[5],[5]"));
		examplePatches.put(OperationType.new_arr, Arrays.asList("[6]", "[6],[6]"));
		examplePatches.put(OperationType.ins_val,
				Arrays.asList("[9,[123,0],[456,1]]", "[9,0,[456,1]]", "[9,[1,0],2]", "[9,1,2]"));
		examplePatches.put(OperationType.ins_obj, Arrays.asList("[10,[1,2],[[\"a\",[3,4]],[\"b\",[5,6]]]]"));
		examplePatches.put(OperationType.ins_vec, Arrays.asList("[11,[1,2],[[2,[3,4]],[0,[5,6]]]]"));
		examplePatches.put(OperationType.ins_arr, Arrays.asList("[14,[1,2],[3,4],[[5,6],[7,8]]]"));
	}

	@Test
	public void testDeserializeAndSerialize() {
		String patchJson = "[[[4,10],{\"key\":9}],[0]]";

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchJson));
		Patch expected = new Patch().setMetadata("{\"key\":9}")
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
				.setOperations(Arrays.asList(
						new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
								.setValue(new ConValue(ConType.UNDEFINED, null))));
		assertEquals(expected, patch);

		// call under test
		String reSerialized = PatchCompactSerializable.serialize(patch).toString();
		assertEquals(patchJson, reSerialized);
	}

	@Test
	public void testDeserializeAndSerializeWithNullMetadat() {
		String patchJson = "[[[4,10]],[0]]";

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchJson));
		Patch expected = new Patch().setMetadata(null)
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
				.setOperations(Arrays.asList(
						new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
								.setValue(new ConValue(ConType.UNDEFINED, null))));
		assertEquals(expected, patch);

		// call under test
		String reSerialized = PatchCompactSerializable.serialize(patch).toString();
		assertEquals(patchJson, reSerialized);
	}

	@Test
	public void testDeserializeAndSerializeMultipleIds() {
		String patchJson = "[[[4,10],{\"key\":9}],[0,8],[0,7],[0,5]]";

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchJson));
		// each sequence number should be incremented by the span.
		Patch expected = new Patch().setMetadata("{\"key\":9}")
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
				.setOperations(Arrays.asList(
						new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
								.setValue(new ConValue(ConType.LONG, 8L)),
						new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(11L))
								.setValue(new ConValue(ConType.LONG, 7L)),
						new NewConstant().setOperationId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(12L))
								.setValue(new ConValue(ConType.LONG, 5L))));
		assertEquals(expected, patch);

		// call under test
		String reSerialized = PatchCompactSerializable.serialize(patch).toString();
		assertEquals(patchJson, reSerialized);
	}

	/**
	 * Insert arrays can have a span larger than one.
	 */
	@Test
	public void testDeserializeAndSerializeWithMultipleInsertArray() {
		String patchJson = "[[[4,10],{\"key\":9}],[14,[1,2],[3,4],[[5,6],[7,8]]],[14,9,10,[11,12,13]],[2]]";

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchJson));
		// each sequence number should be incremented by the span.
		Patch expected = new Patch().setMetadata("{\"key\":9}")
				.setPatchId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L));
		expected.addNewOperation(InsertArray.class)
				.setArrayId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L))
				.setElementIds(Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
						new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L)));
		expected.addNewOperation(InsertArray.class)
				.setArrayId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(9L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L))
				.setElementIds(Arrays.asList(new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(11L),
						new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(12L),
						new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(13L)));
		expected.addNewOperation(NewObject.class);

		assertEquals(expected, patch);
		// The patch has a span of 6 even though there are only three operations.
		assertEquals(6L, patch.getSpan());

		// call under test
		String reSerialized = PatchCompactSerializable.serialize(patch).toString();
		assertEquals(patchJson, reSerialized);
	}

	@Test
	public void testPeekPatchId() {
		String patchJson = "[[[4,10],{\"key\":9}],[0]]";
		// call under test
		LogicalTimestamp patchId = PatchCompactSerializable.peekPatchId(new JSONArray(patchJson));
		LogicalTimestamp expected = new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(10L);
		assertEquals(patchId, expected);
	}

	@ParameterizedTest
	@EnumSource(OperationType.class)
	public void testDeserializeAndSerializeEachType(OperationType type) {
		// we do not currently support these
		if (OperationType.ins_str.equals(type) || OperationType.ins_bin.equals(type) || OperationType.del.equals(type)
				|| OperationType.nop.equals(type)) {
			return;
		}
		examplePatches.get(type).forEach(example -> {
			String patchJson = String.format("[[[4,10],{\"key\":9}],%s]", example);
			// call under test
			Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchJson));
			// call under test
			String reSerialized = PatchCompactSerializable.serialize(patch).toString();
			assertEquals(patchJson, reSerialized);
		});
	}

	@Test
	public void testLoadExampePatchesWithZero() throws IOException {
		String loaded = ClasspathUtil.loadFromClasspath("patch-zero.json");

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(loaded));
		assertNotNull(patch);
		assertEquals(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(1L), patch.getPatchId());
		assertNotNull(patch.getOperations());
		assertEquals(15, patch.getOperations().size());
		Operation last = patch.getOperations().get(patch.getOperations().size() - 1);
		InsertValue expected = new InsertValue()
				.setOperationId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(15L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValueId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(1L));
		assertEquals(expected, last);
	}

	@Test
	public void testLoadExampePatches() throws IOException {
		String loaded = ClasspathUtil.loadFromClasspath("patch-one.json");

		// call under test
		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(loaded));
		assertNotNull(patch);
		assertEquals(new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(16L), patch.getPatchId());
		assertNotNull(patch.getOperations());
		assertEquals(28, patch.getOperations().size());
		Operation last = patch.getOperations().get(patch.getOperations().size() - 1);
		Map<Integer, LogicalTimestamp> map = new LinkedHashMap<Integer, LogicalTimestamp>();
		InsertVector expected = new InsertVector()
				.setOperationId(new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(43L))
				.setVectorId(new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(38L)).setMap(map);
		map.put(0, new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(40L));
		map.put(1, new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(41L));
		map.put(2, new LogicalTimestamp().setReplicaId(65536L).setSequenceNumber(42L));
		assertEquals(expected, last);
	}

}
