package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.sagebionetworks.grid.db.ConstantProvider;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationBuilder;
import org.sagebionetworks.util.ValidateArgument;

public class ChangePatchBuilder implements Closeable, PatchBuilder {

	private static final int EMPTY_PATCH_SIZE_BYTES = 100;

	private final PatchPublisher patchPublisher;
	private final ConstantProvider constantProvider;
	private final GridConnectionInfo connection;
	private final Long maxBytesPerPatch;
	private int currentPatchSize;
	private Patch currentPatch;
	private final Map<String, LogicalTimestamp> constantCache;

	public ChangePatchBuilder(PatchPublisher patchPublisher, ConstantProvider constantProvider,
			GridConnectionInfo connection, LogicalTimestamp currentClock, Long maxBytesPerPatch) {
		this.patchPublisher = patchPublisher;
		this.constantProvider = constantProvider;
		this.connection = connection;
		this.maxBytesPerPatch = maxBytesPerPatch;
		this.constantCache = new HashMap<>();
		this.currentPatch = new Patch().setPatchId(LogicalTimestamp.newIncrement(currentClock, 1));
		this.currentPatchSize = EMPTY_PATCH_SIZE_BYTES;
	}

	@Override
	public <T extends Operation<T>> LogicalTimestamp addOperationBuilder(OperationBuilder<T> builder) {
		ValidateArgument.required(builder, "builder");

		if (builder instanceof NewConstantBuilder) {
			return addConstantOperation((NewConstantBuilder) builder);
		}

		return addRegularOperation(builder);
	}

	LogicalTimestamp addConstantOperation(NewConstantBuilder builder) {
		ValidateArgument.required(builder.getValue(), "builder.value");
		String jsonValue = builder.getValue().toJson();

		// Return cached constant if exists
		if (constantCache.containsKey(jsonValue)) {
			return constantCache.get(jsonValue);
		}

		// Check database for existing constant
		Optional<LogicalTimestamp> existing = constantProvider.findExistingConstant(connection.getSessionId(),
				connection.getReplicaId(), jsonValue);

		if (existing.isPresent()) {
			constantCache.put(jsonValue, existing.get());
			return existing.get();
		}

		// Add new constant to patch
		LogicalTimestamp timestamp = addToPatch(builder);
		constantCache.put(jsonValue, timestamp);
		return timestamp;
	}

	LogicalTimestamp addRegularOperation(OperationBuilder<?> builder) {
		return addToPatch(builder);
	}

	LogicalTimestamp addToPatch(OperationBuilder<?> builder) {
		int bytes = PatchCompactSerializable.calculateOperationSizeBytes(builder);

		if (currentPatchSize + bytes > maxBytesPerPatch) {
			saveCurrentPatch();
		}

		currentPatchSize += bytes;
		return currentPatch.addNewOperation(builder);
	}

	@Override
	public void close() throws IOException {
		saveCurrentPatch();
	}

	void saveCurrentPatch() {
		if (currentPatch.getOperations() == null || currentPatch.getOperations().isEmpty()) {
			return;
		}

		JSONArray patchBody = PatchCompactSerializable.serialize(currentPatch);
		patchPublisher.publishPatch(connection, patchBody);

		currentPatch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(currentPatch.getPatchId(), currentPatch.getSpan()));
		currentPatchSize = EMPTY_PATCH_SIZE_BYTES;
	}

	public Long getMaxBytesPerPatch() {
		return maxBytesPerPatch;
	}

}
