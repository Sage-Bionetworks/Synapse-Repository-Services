package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.filter.VectorIdViewFilter;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridReplicaValidationManagerImpl implements GridReplicaValidationManager {

	private final GridReplicaViewManager gridReplicaViewManager;
	private final JsonSchemaManager jsonSchemaManager;
	private final GridDao gridDao;
	private final JsonSchemaValidationManager jsonSchemaValidationManager;
	private final PatchBuilderPublisher patchBuilderPublisher;

	public GridReplicaValidationManagerImpl(GridReplicaViewManager gridReplicaViewManager,
			JsonSchemaManager jsonSchemaManager, GridDao gridDao,
			JsonSchemaValidationManager jsonSchemaValidationManager, PatchBuilderPublisher patchBuilderPublisher) {
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.jsonSchemaManager = jsonSchemaManager;
		this.gridDao = gridDao;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.patchBuilderPublisher = patchBuilderPublisher;
	}

	@Override
	public void validateChanges(String sessionId, Long replicaId, String connectionId,
			Collection<LogicalTimestamp> changedVectorIds) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(connectionId, "connectionId");

		if (changedVectorIds == null || changedVectorIds.isEmpty()) {
			return;
		}

		Optional<GridSession> gridSession = gridDao.getGridSession(sessionId);
		if (!hasValidSession(gridSession)) {
			return;
		}

		Optional<GridHeader> header = gridReplicaViewManager.readHeader(sessionId, replicaId);
		if (header.isEmpty()) {
			return;
		}

		List<RowView> rowsToValidate = getRowsToValidate(header.get(), changedVectorIds);
		if (rowsToValidate.isEmpty()) {
			return;
		}

		List<IntendedChange> intendedChanges = validateRows(header.get(), gridSession.get().getGridJsonSchema$Id(),
				rowsToValidate);
		if (intendedChanges.isEmpty()) {
			return;
		}

		// send the changes to the patch builder.
		patchBuilderPublisher.sendChangesToPatchBuilder(new IntendedChangeSet().setChanges(intendedChanges)
				.setSessionId(sessionId).setReplicaId(replicaId).setConnectionId(connectionId));
	}

	boolean hasValidSession(Optional<GridSession> gridSession) {
		return gridSession.isPresent() && gridSession.get().getGridJsonSchema$Id() != null;
	}

	/**
	 * Run a query to fetch the rows identified by the provided vector IDs.
	 * 
	 * @param header
	 * @param changedVectorIds
	 * @return
	 */
	List<RowView> getRowsToValidate(GridHeader header, Collection<LogicalTimestamp> changedVectorIds) {
		List<LogicalTimestamp> vectorList = changedVectorIds.stream().collect(Collectors.toList());
		Long limit = (long) (changedVectorIds.size() + 1);
		return gridReplicaViewManager.querySinglePage(header, List.of(new VectorIdViewFilter(vectorList)), limit, 0L);
	}

	/**
	 * Validate a set of rows and generate changes to be sent to the patch builder.
	 * 
	 * @param header
	 * @param schemaId
	 * @param rowsToValidate
	 * @return
	 */
	List<IntendedChange> validateRows(GridHeader header, String schemaId, List<RowView> rowsToValidate) {
		JsonSchema schema = jsonSchemaManager.getValidationSchema(schemaId);

		return rowsToValidate.stream().map(row -> validateCells(header, schema, row)).filter(Optional::isPresent)
				.map(Optional::get).collect(Collectors.toList());
	}

	/**
	 * Validate the cell data for a single row against the provided schema.
	 * 
	 * @param header
	 * @param schema
	 * @param row
	 * @return Optional.empty() if the new results are equal to the old.
	 */
	Optional<IntendedChange> validateCells(GridHeader header, JsonSchema schema, RowView row) {
		RowJsonSubject subject = new RowJsonSubject(header.getOrderedColumns(), row);
		ValidationResults validationResults = jsonSchemaValidationManager.validate(schema, subject);

		cleanupValidationResults(validationResults);

		if (validationResults.equals(row.getRowValidationResults())) {
			return Optional.empty();
		}

		return Optional.of(createChange(row, validationResults));
	}

	/**
	 * Remove 'extra' data from a row's validation results to reduce its size.
	 * 
	 * @param validationResults
	 */
	void cleanupValidationResults(ValidationResults validationResults) {
		validationResults.setValidatedOn(null);
		validationResults.setSchema$id(null);
	}

	/**
	 * Helper to build a new IntendedChange for row with new validation results.
	 * 
	 * @param rowView
	 * @param newValidationResults
	 * @return
	 */
	IntendedChange createChange(RowView rowView, ValidationResults newValidationResults) {
		try {
			RowObject rowObject = rowView.getRowObject();
			RowMetadata rowMetadata = rowView.getRowMetadata();

			return new UpdateMetadataChange()
					.setValidationState(EntityFactory.createJSONObjectForEntity(newValidationResults))
					.setRowObjectId(rowObject != null ? rowObject.getObjectId() : null)
					.setRowMetadataId(rowMetadata != null ? rowMetadata.getObjectId() : null);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
}
