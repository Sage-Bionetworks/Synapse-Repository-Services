package org.sagebionetworks.repo.model.grid.patch.compact;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.OperationBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;
import org.sagebionetworks.util.ValidateArgument;

public class PatchCompactSerializable {

	private static List<OperationSerializable<?>> serializables = Arrays.asList(new NewConstantSerializable(),
			new NewValueSerializable(), new NewObjectSerializable(), new NewVectorSerializable(),
			new NewStringSerializable(), new NewBinarySerializable(), new NewArraySerializable(),
			new InsertValueSerializable(), new InsertObjectSerializable(), new InsertVectorSerializable(),
			new InsertArraySerializable(), new DeleteSerializable());

	private static Map<OperationType, OperationSerializable<?>> map = serializables.stream()
			.collect(Collectors.toMap(OperationSerializable::getType, handler -> handler));

	/**
	 * Deserialize a {@link Patch} from a compact JSON array.
	 * 
	 * @param compact
	 * @return
	 */
	public static Patch deserialize(JSONArray compact) {
		Patch patch = new Patch();
		// first array is the patch metadata
		JSONArray header = compact.getJSONArray(0);

		patch.setPatchId(LogicalTimestampCompactSerializable.deserialize(header.getJSONArray(0)));
		JSONObject metadata = header.optJSONObject(1);
		patch.setMetadata(metadata != null ? metadata.toString() : null);
		List<Operation> operations = new ArrayList<>(compact.length() - 1);
		// The first operation ID is the same as the patchId.
		LogicalTimestamp nextOperationId = LogicalTimestamp.clone(patch.getPatchId());
		for (int i = 0; i < compact.length() - 1; i++) {
			JSONArray next = compact.getJSONArray(i + 1);
			int code = next.getInt(0);
			OperationType type = OperationType.fromCode(code);
			OperationSerializable<?> serializer = map.get(type);
			if (serializer == null) {
				throw new IllegalArgumentException("Unknown type: " + type);
			}
			Operation operation = serializer.deserialize(nextOperationId, next);
			operations.add(operation);
			nextOperationId = LogicalTimestamp.newIncrement(nextOperationId, operation.getSpan());
		}
		patch.setOperations(operations);
		return patch;
	}

	/**
	 * Read the patch ID for a patch without deserialization.
	 * 
	 * @return
	 */
	public static LogicalTimestamp peekPatchId(JSONArray compact) {
		ValidateArgument.required(compact, "array");
		JSONArray header = compact.getJSONArray(0);
		return LogicalTimestampCompactSerializable.deserialize(header.getJSONArray(0));
	}

	/**
	 * Serialize a {@link Patch} to a compact JSON array.
	 * 
	 * @param patch
	 * @return
	 */
	public static JSONArray serialize(Patch patch) {
		JSONArray compact = new JSONArray();
		JSONArray header = new JSONArray().put(0, LogicalTimestampCompactSerializable.serialize(patch.getPatchId()));
		if (patch.getMetadata() != null) {
			header.put(1, new JSONObject(patch.getMetadata()));
		}
		compact.put(0, header);
		for (int i = 0; i < patch.getOperations().size(); i++) {
			Operation op = patch.getOperations().get(i);
			OperationSerializable<?> serializer = map.get(op.getType());
			if (serializer == null) {
				throw new IllegalArgumentException("Unknown type: " + op.getType());
			}
			JSONArray serializedOperation = serializeWithHelper(serializer, op);
			compact.put(serializedOperation);
		}

		return compact;
	}

	/**
	 * Calculate the number of bytes needed to serialize the given operation
	 * builder, assuming it is issued an operation ID with the maximum size.
	 * 
	 * @param <T>
	 * @param builder
	 * @return
	 */
	public static <T extends Operation> int calculateOperationSizeBytes(OperationBuilder builder) {
		Operation op = builder.build(new LogicalTimestamp().setReplicaId(Long.MAX_VALUE).setSequenceNumber(Long.MAX_VALUE));
		OperationSerializable<T> serializer = (OperationSerializable<T>) map.get(op.getType());
		JSONArray arr = serializer.serialize((T) op);
		return arr.toString().getBytes(StandardCharsets.UTF_8).length;
	}

	/**
	 * Catpures the generic of OperationSerializable and serializes.
	 * 
	 * @param <S>
	 * @param typedSerializer
	 * @param patchId
	 * @param index
	 * @param generalOperation
	 * @return
	 */
	private static <S extends Operation> JSONArray serializeWithHelper(OperationSerializable<S> typedSerializer, Operation generalOperation) {
		S specificOperation = typedSerializer.getTypeClass().cast(generalOperation);
		return typedSerializer.serialize(specificOperation);
	}

}
