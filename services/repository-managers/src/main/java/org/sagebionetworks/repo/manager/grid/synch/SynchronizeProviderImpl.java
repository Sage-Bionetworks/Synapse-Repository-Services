package org.sagebionetworks.repo.manager.grid.synch;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopy;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMerge;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMergeImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSource;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSourceImpl;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaCopy;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaCopyImpl;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSource;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSourceImpl;
import org.springframework.stereotype.Service;

@Service
public class SynchronizeProviderImpl implements SynchronizeProvider {

	@Override
	public SchemaCopy getSchemaCopy(IntendedChangePublisher intendedChangePublisher, CopyHandler reader) {
		return new SchemaCopyImpl(intendedChangePublisher, reader);
	}

	@Override
	public SchemaSource getSchemaSource(SourceHandler handler) {
		return new SchemaSourceImpl(handler);
	}

	@Override
	public RowCopy getRowCopy(IntendedChangePublisher intendedChangePublisher, List<Column> finalSchema,
			CopyHandler reader) {
		return new RowCopyImpl(finalSchema, intendedChangePublisher, reader);
	}

	@Override
	public RowSource getRowSource(RowSourceItemReader sourceReader, SourceHandler handler) {
		return new RowSourceImpl(handler, sourceReader);
	}

	@Override
	public RowMerge getRowMerge(SynchronizationLogic logic, IntendedChangePublisher intendedChangePublisher,
			List<Column> finalSchema, CopyHandler reader, SourceHandler handler) {
		return new RowMergeImpl(logic, handler, intendedChangePublisher, reader, finalSchema);
	}

}
