package org.sagebionetworks.repo.manager.grid.synch;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncRules;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncRules;

/**
 * Factory for the per-run collaborators consumed by {@link SynchronizationLogic}:
 * the read-only {@code SourceReader}, the {@code SyncRules}, and the
 * {@code SyncOutcomeHandler} at both the schema (Phase 1) and row (Phase 2)
 * granularities.
 */
public interface SynchronizeProvider {

	/**
	 * Creates the read-only view of the source schema for Phase 1.
	 */
	SchemaSourceReader getSchemaSourceReader(SourceHandler handler);

	/**
	 * Creates the Phase 1 schema keying/matching rules.
	 */
	SchemaSyncRules getSchemaSyncRules(SourceHandler handler);

	/**
	 * Creates the Phase 1 schema outcome handler, which is responsible for
	 * applying schema changes to the copy and the source, and tracks the
	 * reconciled final schema.
	 */
	SchemaSyncOutcomeHandler getSchemaSyncOutcomeHandler(IntendedChangePublisher intendedChangePublisher,
			CopyHandler copyHandler, SourceWriter sourceWriter);

	/**
	 * Creates the read-only view of the source rows for Phase 2.
	 */
	RowSourceReader getRowSourceReader(RowSourceItemReader sourceReader);

	/**
	 * Creates the Phase 2 row keying/matching rules.
	 */
	RowSyncRules getRowSyncRules(SourceHandler handler);

	/**
	 * Creates the Phase 2 row outcome handler, which is responsible for resolving
	 * row conflicts via a nested cell synchronization and applying changes to the copy
	 * and the source.
	 *
	 * @param preserveUserAttribution when true (PULL), user-changed cells are not
	 *                                rewritten in the grid, preserving attribution
	 */
	RowSyncOutcomeHandler getRowSyncOutcomeHandler(SynchronizationLogic logic, IntendedChangePublisher intendedChangePublisher, List<Column> finalSchema, CopyHandler copyHandler, SourceWriter sourceWriter, boolean preserveUserAttribution);

}
