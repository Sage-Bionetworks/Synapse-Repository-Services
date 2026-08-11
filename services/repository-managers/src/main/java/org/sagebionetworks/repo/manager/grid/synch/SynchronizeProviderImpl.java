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
import org.springframework.stereotype.Service;

@Service
public class SynchronizeProviderImpl implements SynchronizeProvider {

	@Override
	public SchemaSourceReader getSchemaSourceReader(SourceHandler handler) {
		return new SchemaSourceReader(handler);
	}

	@Override
	public SchemaSyncRules getSchemaSyncRules(SourceHandler handler) {
		return new SchemaSyncRules(handler);
	}

	@Override
	public SchemaSyncOutcomeHandler getSchemaSyncOutcomeHandler(IntendedChangePublisher intendedChangePublisher,
			CopyHandler copyHandler, SourceWriter sourceWriter) {
		return new SchemaSyncOutcomeHandler(intendedChangePublisher, copyHandler, sourceWriter);
	}

	@Override
	public RowSourceReader getRowSourceReader(RowSourceItemReader sourceReader) {
		return new RowSourceReader(sourceReader);
	}

	@Override
	public RowSyncRules getRowSyncRules(SourceHandler handler) {
		return new RowSyncRules(handler);
	}

	@Override
	public RowSyncOutcomeHandler getRowSyncOutcomeHandler(SynchronizationLogic logic, IntendedChangePublisher intendedChangePublisher,
			List<Column> finalSchema, CopyHandler copyHandler, SourceWriter sourceWriter, boolean preserveUserAttribution) {
		return new RowSyncOutcomeHandler(logic, intendedChangePublisher, copyHandler, sourceWriter,
				finalSchema, preserveUserAttribution);
	}

}
