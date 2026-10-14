package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridTransaction;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.VectorIdFilterElement;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterators;

@Service
public class GridReplicaValidationManagerImpl implements GridReplicaValidationManager {

	private static final Logger log = LogManager.getLogger(GridReplicaValidationManagerImpl.class);

	private final GridReplicaViewManager gridReplicaViewManager;
	private final GridDao gridDao;
	private final GridRowValidator gridRowValidator;
	private final PatchBuilderPublisher patchBuilderPublisher;

	public GridReplicaValidationManagerImpl(GridReplicaViewManager gridReplicaViewManager, GridDao gridDao,
			GridRowValidator gridRowValidator, PatchBuilderPublisher patchBuilderPublisher) {
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.gridDao = gridDao;
		this.gridRowValidator = gridRowValidator;
		this.patchBuilderPublisher = patchBuilderPublisher;
	}

	@GridTransaction(readOnly = true)
	@Override
	public void validateAllRows(String sessionId, Long replicaId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");

		Optional<GridSession> gridSession = resolveValidSession(sessionId, "validation");
		if (gridSession.isEmpty()) {
			return;
		}

		Optional<GridConnectionInfo> validationConnectionOpt = gridDao.getSingletonConnection(sessionId,
				EventSource.VALIDATION);
		if (validationConnectionOpt.isEmpty()) {
			log.info("No validation connection found for sessionId: {}, skipping validation", sessionId);
			return;
		}

		Optional<GridHeader> header = gridReplicaViewManager.readHeader(sessionId, replicaId);
		if (header.isEmpty()) {
			log.info("No grid header found for sessionId: {}, replicaId: {}, skipping validation", sessionId, replicaId);
			return;
		}

		// For each row in the replica, validate only if the data is newer than the validation result.
		validateRowsInBatches(gridSession.get(), header.get(), validationConnectionOpt.get(),
				this::isDataNewerThanValidationResult);
	}

	/**
	 * Force a full revalidation of every row in the session, because the
	 * session's bound JSON schema changed — unlike {@link #validateAllRows},
	 * this validates every row unconditionally, since the schema (not the data)
	 * is what changed.
	 * <p>
	 * Throws {@link RecoverableMessageException} if the connection to the
	 * VALIDATION replica is not yet available
	 *
	 * @param sessionId
	 */
	@GridTransaction(readOnly = true)
	@Override
	public void validateAfterSchemaChange(String sessionId) {
		ValidateArgument.required(sessionId, "sessionId");

		Optional<GridSession> gridSession = resolveValidSession(sessionId, "schema-change validation");
		if (gridSession.isEmpty()) {
			return;
		}

		Optional<GridConnectionInfo> internalConnectionOpt = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL);
		if (internalConnectionOpt.isEmpty()) {
			log.info("No internal connection found for sessionId: {}, skipping schema-change validation", sessionId);
			return;
		}

		Optional<GridConnectionInfo> validationConnectionOpt = gridDao.getSingletonConnection(sessionId,
				EventSource.VALIDATION);
		if (validationConnectionOpt.isEmpty()) {
			throw new RecoverableMessageException(
					"A VALIDATION replica connection is being established for sessionId: " + sessionId
							+ ", retrying once it has connected.");
		}

		Optional<GridHeader> header = gridReplicaViewManager.readHeader(sessionId,
				internalConnectionOpt.get().getReplicaId());
		if (header.isEmpty()) {
			log.info("No grid header found for sessionId: {}, skipping schema-change validation", sessionId);
			return;
		}

		validateRowsInBatches(gridSession.get(), header.get(), validationConnectionOpt.get(), row -> true);
	}

	/**
	 * Load the session and verify it is valid for validation (present and bound
	 * to a JSON schema). Logs and returns {@link Optional#empty()} when the
	 * session cannot be validated.
	 *
	 * @param sessionId
	 * @param action    included in the skip log message for context
	 * @return the valid session, or empty if validation should be skipped
	 */
	Optional<GridSession> resolveValidSession(String sessionId, String action) {
		Optional<GridSession> gridSession = gridDao.getGridSession(sessionId);
		if (!hasValidSession(gridSession)) {
			log.info("No valid grid session found for sessionId: {}, skipping {}", sessionId, action);
			return Optional.empty();
		}
		return gridSession;
	}

	/**
	 * Iterate every row in the header in batches, validate the rows that pass the
	 * {@code shouldValidate} predicate, and publish the resulting changes via the
	 * given (VALIDATION) connection.
	 *
	 * @param gridSession
	 * @param header
	 * @param validationConnection
	 * @param shouldValidate       a row is validated only when this predicate is
	 *                             true for it; pass {@code row -> true} to
	 *                             validate every row unconditionally.
	 */
	void validateRowsInBatches(GridSession gridSession, GridHeader header, GridConnectionInfo validationConnection,
			Predicate<RowView> shouldValidate) {
		Iterator<RowView> rowViewIterator = gridReplicaViewManager.getQueryIterator(header, List.of());

		try (IntendedChangePublisher publisher = new IntendedChangePublisher(
				validationConnection,
				header.getClockSequenceMaximum(),
				patchBuilderPublisher,
				PatchUtils.MAX_CHANGE_SET_SIZE)) {

			Iterators.partition(rowViewIterator, 1000).forEachRemaining(batch -> {
				List<RowView> rowsToValidate = batch.stream().filter(shouldValidate).collect(Collectors.toList());
				if (rowsToValidate.isEmpty()) {
					return;
				}

				List<IntendedChange> intendedChanges = validateRows(header, gridSession.getGridJsonSchema$Id(),
						rowsToValidate);

				for (IntendedChange change : intendedChanges) {
					publisher.publish(change);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@GridTransaction(readOnly = true)
	@Override
	public void validateChanges(String sessionId, Long replicaId, Collection<LogicalTimestamp> changedVectorIds) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");

		if (changedVectorIds == null || changedVectorIds.isEmpty()) {
			log.info("No changed vector IDs provided, skipping validation for sessionId: {}, replicaId: {}",
					sessionId, replicaId);
			return;
		}

		Optional<GridSession> gridSession = resolveValidSession(sessionId, "validation");
		if (gridSession.isEmpty()) {
			return;
		}

		Optional<GridConnectionInfo> validationConnectionOpt = gridDao.getSingletonConnection(sessionId,
				EventSource.VALIDATION);
		if (validationConnectionOpt.isEmpty()) {
			log.info("No validation connection found for sessionId: {}, skipping validation", sessionId);
			return;
		}

		Optional<GridHeader> header = gridReplicaViewManager.readHeader(sessionId, replicaId);
		if (header.isEmpty()) {
			log.info("No grid header found for sessionId: {}, replicaId: {}, skipping validation", sessionId, replicaId);
			return;
		}

		List<RowView> rowsToValidate = getRowsToValidate(header.get(), changedVectorIds);
		if (rowsToValidate.isEmpty()) {
			log.info("No rows to validate for sessionId: {}, replicaId: {}", sessionId, replicaId);
			return;
		}

		List<IntendedChange> intendedChanges = validateRows(header.get(), gridSession.get().getGridJsonSchema$Id(),
				rowsToValidate);
		if (intendedChanges.isEmpty()) {
			log.info("No validation changes generated for sessionId: {}, replicaId: {}", sessionId, replicaId);
			return;
		}

		try (IntendedChangePublisher publisher = new IntendedChangePublisher(
				validationConnectionOpt.get(),
				header.get().getClockSequenceMaximum(),
				patchBuilderPublisher,
				PatchUtils.MAX_CHANGE_SET_SIZE)) {

			for (IntendedChange change : intendedChanges) {
				publisher.publish(change);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		List<RowView> rowsToValidate = gridReplicaViewManager.querySinglePage(header, List.of(new VectorIdFilterElement(vectorList)), limit, 0L);

		/*
		 * Filter out rows where the validation result is up-to-date based on the timestamp IDs.
		 * This is necessary because:
		 *   1. Snapshots may or may not contain validation information (it may be a "new" grid that is pending validation)
		 *   2. Importing a snapshot triggers a change message for every node in the replica's document
		 *   3. We _cannot_ compare new validation results to the previous to opt out of an update, because we
		 * 		 unconditionally update validation data on a change so the client can identify if validation information
		 *       is up-to-date (PLFM-9342)
		 *
		 *  We may in the future automatically create new snapshots to capture changes that happened since the last
		 * 	 snapshot. Without this opt-out, that would cause an infinite loop where new snapshots would cause validation
		 *   result constants to unnecessarily update, which could trigger/be included in a new snapshot.
		 */
		return rowsToValidate.stream()
				.filter(this::isDataNewerThanValidationResult)
				.collect(Collectors.toList());
	}

	/**
	 * Using the timestamp of the data and the validation results, determine if the data is newer than the validation results.
	 * If so, we need to re-validate.
	 * @param rowView
	 * @return true if the data is newer than the validation results.
	 */
	boolean isDataNewerThanValidationResult(RowView rowView) {
		RowData rowData = rowView.getRowObject().getData();
		RowMetadata metadata = rowView.getRowMetadata();
		if (metadata == null || metadata.getRowValidation() == null || metadata.getRowValidation().getConstantId() == null) {
			// If there are no validation results, always validate.
			return true;
		}

		// Otherwise, check all of the constant IDs in the validation results.
		return Arrays.stream(rowData.getNodes())
				.filter(Objects::nonNull)
				.map(ConstantNode::getId)
				// If any are greater than the current validation timestamp, re-validate.
				.anyMatch(id -> id != null && id.compareTo(metadata.getRowValidation().getConstantId()) > 0);
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
		JsonSchema schema = gridRowValidator.getValidationSchema(schemaId);

		List<JsonSubject> subjects = rowsToValidate.stream()
				.map(row -> new JsonObjectSubject(row.getRowObject().getData().getRowJsonDocument())).collect(Collectors.toList());

		List<ValidationResults> results = gridRowValidator.validateBatch(schema, subjects);

		List<IntendedChange> changes = new ArrayList<>();

		for (int i = 0; i < results.size(); i++) {
			ValidationResults validationResults = results.get(i);
			RowView row = rowsToValidate.get(i);

			// Always apply the new validation results, even if the value did not change.
			// The client uses the timestamp to determine if results are up-to-date with its local changes.
			changes.add(createChange(row, validationResults));
		}

		return changes;
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