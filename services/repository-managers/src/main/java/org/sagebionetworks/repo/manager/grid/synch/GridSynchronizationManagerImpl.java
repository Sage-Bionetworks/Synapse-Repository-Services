package org.sagebionetworks.repo.manager.grid.synch;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandlerProvider;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandlerProvider;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncRules;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncRules;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridSynchronizationManagerImpl implements GridSynchronizationManager {

	private final GridManager gridManager;
	private final PatchBuilderPublisher patchBuilderPublisher;
	private final SourceHandlerProvider sourceHandlerProvider;
	private final CopyHandlerProvider copyHandlerProvider;
	private final SynchronizationLogic logic;
	private final SynchronizeProvider synchronizeProvider;

	public GridSynchronizationManagerImpl(SourceHandlerProvider sourceHandlerProvider,
			CopyHandlerProvider copyHandlerProvider, SynchronizationLogic logic,
			SynchronizeProvider synchronizeProvider, PatchBuilderPublisher patchBuilderPublisher,
			GridManager gridManager) {
		super();
		this.gridManager = gridManager;
		this.patchBuilderPublisher = patchBuilderPublisher;
		this.sourceHandlerProvider = sourceHandlerProvider;
		this.copyHandlerProvider = copyHandlerProvider;
		this.logic = logic;
		this.synchronizeProvider = synchronizeProvider;
	}

	@Override
	public SynchronizeGridResponse synchronizeCopyWithSource(AsyncJobProgressCallback callback, UserInfo user,
			SynchronizeGridRequest request) throws Exception {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getGridSessionId(), "request.gridSessionId");
		GridSession session = gridManager.getGridSession(user, request.getGridSessionId());

		// If not provided, the default SyncType is PULL_PUSH
		SyncType syncType = Optional.ofNullable(request.getSyncType()).orElse(SyncType.PULL_PUSH);

		List<String> errorMessage;
		Set<Long> benefactorIds;
		try (CopyHandler copyHandler = copyHandlerProvider.createCopyHandler(session);
				SourceHandler sourceHandler = sourceHandlerProvider.createNewHandler(callback, user, session,
						copyHandler.getGridSource());
				SourceWriter sourceWriter = sourceHandler.createSourceWriter(syncType);
				RowSourceItemReader sourceReader = sourceHandler.getSourceRowReader();
				IntendedChangePublisher icp = newIntendedChangePublisher(copyHandler)) {

			// Check if this syncType is supported by the source
			sourceHandler.validateSyncType(syncType);

			// Phase one: synchronize the schema
			List<Column> finalSchema;
			try (SchemaSyncOutcomeHandler schemaHandler = synchronizeProvider.getSchemaSyncOutcomeHandler(icp,
					copyHandler, sourceWriter)) {
				SchemaSourceReader schemaReader = synchronizeProvider.getSchemaSourceReader(sourceHandler);
				SchemaSyncRules schemaRules = synchronizeProvider.getSchemaSyncRules(sourceHandler);
				logic.synchronize(schemaHandler.streamCopyItems(), schemaReader, schemaRules, schemaHandler);
				finalSchema = schemaHandler.getFinalSchema();
			}

			// On a PULL (no write-back to source) the merge must not rewrite cells the user
			// changed, otherwise their CRDT nodes are re-minted under the service replica and
			// lose their user-attribution, causing a subsequent PULL to revert the edit.
			boolean preserveUserAttribution = SyncType.PULL.equals(syncType);

			// Prepare any push artifact the writer may build during the merge
			sourceWriter.beginPush(callback, finalSchema);

			// Phase two: run the row merge. The row outcome handler applies grid CRDT
			// changes directly and reports every surviving row to the writer so a pushed
			// artifact can capture the full final grid contents.
			RowSourceReader rowReader = synchronizeProvider.getRowSourceReader(sourceReader);
			RowSyncRules rowRules = synchronizeProvider.getRowSyncRules(sourceHandler);
			RowSyncOutcomeHandler rowHandler = synchronizeProvider.getRowSyncOutcomeHandler(logic, icp, finalSchema, copyHandler,
					sourceWriter, preserveUserAttribution);
			logic.synchronize(rowHandler.streamCopyItems(), rowReader, rowRules, rowHandler);

			errorMessage = sourceWriter.getErrorMessages();
			benefactorIds = sourceHandler.getBenefactorIds();

			// Record the source revision the grid is now synchronized to (the new
			// baseline version) for deletion detection on subsequent syncs.
			sourceHandler.getSourceVersion()
					.ifPresent(version -> gridManager.updateSourceEntityVersion(session.getSessionId(), version));

			// Record the source's current bound JSON schema $id, so subsequent row
			// validation runs against the schema the grid was synchronized to.
			sourceHandler.getSourceSchema$Id()
					.ifPresent(schemaId -> gridManager.updateSessionSchemaId(session.getSessionId(), schemaId));

			// Flush the push if applicable. The writer writes the artifact back as a new
			// version (RecordSet PULL_PUSH only; other cases are no-ops). The new source
			// version supersedes the version recorded above.
			sourceWriter.completePush()
					.ifPresent(version -> gridManager.updateSourceEntityVersion(session.getSessionId(), version));
		}
		// Update benefactor IDs and evict any connections that no longer have access.
		gridManager.updateSessionBenefactorIds(session.getSessionId(), benefactorIds);
		return new SynchronizeGridResponse().setErrorMessages(errorMessage).setGridSessionId(session.getSessionId());
	}

	IntendedChangePublisher newIntendedChangePublisher(CopyHandler copyHandler) {
		GridHeader header = copyHandler.getHeader();
		GridConnectionInfo connInfo = copyHandler.getConnectionInfo();
		return new IntendedChangePublisher(connInfo, header.getClockSequenceMaximum(), patchBuilderPublisher,
				PatchUtils.MAX_CHANGE_SET_SIZE);
	}
}
