package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.io.Closeable;
import java.io.IOException;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.OperationBuilder;
import org.sagebionetworks.util.ValidateArgument;

public class ChangePatchBuilder implements Closeable, PatchBuilder {

	private static final int EMPTY_PATCH_SIZE_BYTES = 100;

	private final PatchPublisher patchPublisher;
	private final GridConnectionInfo connection;
	private final Long maxBytesPerPatch;
	private int currentPatchSize;
	private Patch currentPatch;

	public ChangePatchBuilder(PatchPublisher patchPublisher, GridConnectionInfo connection,
			  LogicalTimestamp currentClock, Long maxBytesPerPatch) {
		this.patchPublisher = patchPublisher;
		this.connection = connection;
		this.maxBytesPerPatch = maxBytesPerPatch;
		this.currentPatch = new Patch().setPatchId(LogicalTimestamp.newIncrement(currentClock, 1));
		this.currentPatchSize = EMPTY_PATCH_SIZE_BYTES;
	}

	@Override
	public LogicalTimestamp addOperationBuilder(OperationBuilder builder) {
		ValidateArgument.required(builder, "builder");

		if (builder instanceof NewConstantBuilder) {
			return addConstantOperation((NewConstantBuilder) builder);
		}

		return addRegularOperation(builder);
	}

	LogicalTimestamp addConstantOperation(NewConstantBuilder builder) {
		ValidateArgument.required(builder.getValue(), "builder.value");
		// Add new constant to patch
		LogicalTimestamp timestamp = addToPatch(builder);
		return timestamp;
	}

	LogicalTimestamp addRegularOperation(OperationBuilder builder) {
		return addToPatch(builder);
	}

	LogicalTimestamp addToPatch(OperationBuilder builder) {
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
		patchPublisher.publishPatch(connection, patchBody, currentPatch.getSpan());

		currentPatch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(currentPatch.getPatchId(), currentPatch.getSpan()));
		currentPatchSize = EMPTY_PATCH_SIZE_BYTES;
	}

	public Long getMaxBytesPerPatch() {
		return maxBytesPerPatch;
	}

}
