package org.sagebionetworks.repo.manager.grid.create;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.IndexedModelEncoderProvider;
import org.sagebionetworks.repo.manager.grid.SnapshotRowHandler;
import org.sagebionetworks.repo.manager.grid.SnapshotStore;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaConnectionManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.FileProvider;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class RecordSetCreateGridHandler implements CreateGridHandler {

	private final GridDao gridDao;
	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;
	private final EntityAuthorizationManager authorizationManager;
	private final CsvFileHandleProvider csvProvider;
	private final GridRowValidator gridRowValidator;
	private final FileProvider fileProvider;
	private final IndexedModelEncoderProvider encoderProvider;
	private final RecordSetSchemaResolver schemaResolver;
	private final GridReplicaConnectionManager gridReplicaConnectionManager;

	public static final CsvTableDescriptor DEFAULT_RECORD_SET_CSV_DESCRIPTOR = new CsvTableDescriptor().setIsFirstLineHeader(true);

	public RecordSetCreateGridHandler(GridDao gridDao, EntityManager entityManager, FileHandleManager fileHandleManager,
									  EntityAuthorizationManager authorizationManager, CsvFileHandleProvider csvProvider,
									  GridRowValidator gridRowValidator,
									  FileProvider fileProvider, IndexedModelEncoderProvider encoderProvider,
									  RecordSetSchemaResolver schemaResolver,
									  GridReplicaConnectionManager gridReplicaConnectionManager) {
		super();
		this.gridDao = gridDao;
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
		this.authorizationManager = authorizationManager;
		this.csvProvider = csvProvider;
		this.gridRowValidator = gridRowValidator;
		this.fileProvider = fileProvider;
		this.encoderProvider = encoderProvider;
		this.schemaResolver = schemaResolver;
		this.gridReplicaConnectionManager = gridReplicaConnectionManager;
	}

	@Override
	public boolean canCreate(CreateGridRequest request) {
		return request.getRecordSetId() != null;
	}

	@Override
	public CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			SnapshotStore snapshotStore) {
		String recordSetId = request.getRecordSetId();
		
		RecordSet recordSet = entityManager.getEntity(user, recordSetId, RecordSet.class);
		
		// Makes sure the user has download access
		authorizationManager.hasAccess(user, recordSet.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		
		final Optional<String> validationSchemaId = entityManager.findBoundSchema(recordSetId)
				.map(binding -> binding.getJsonSchemaVersionInfo().get$id());

		GridSession session = gridDao.createGridSession(new CreateGridSession().setUserId(user.getId())
				.setSourceId(recordSet.getId()).setSourceVersion(recordSet.getVersionNumber())
				.setSchemaId(validationSchemaId.orElse(null))
				.setOwner(request.getOwnerPrincipalId()).setAuthorizationMode(request.getAuthorizationMode()));

		GridReplica replica = gridReplicaConnectionManager.createReplicaAndConnect(user.getId(), session.getSessionId(), false, EventSource.INTERNAL);

		// We already checked that the user has download access
		FileHandle fileHandle = fileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId());

		CsvTableDescriptor csvDescriptor = recordSet.getCsvDescriptor();

		if (csvDescriptor == null) {
			csvDescriptor = DEFAULT_RECORD_SET_CSV_DESCRIPTOR;
		}

		// In order to emit patches using the PatchRowHandler we need a starting schema,
		// this is needed so that the values in a row are emitted with some sensible data
		// types. Additionally, we split into multiple patches according to the max size
		// of each row.
		//
		// The schema is inferred from the CSV file (full scan so the row size is
		// accurate) and reconciled with the bound JSON Schema (PLFM-9558); we also
		// compute the indices of the columns the JSON Schema marks as required. This is
		// the same inference the RecordSetMetadataProvider binds at create/update time.
		RecordSetSchemaResolver.ReconciledSchema reconciled = schemaResolver
				.getReconciledSchema(recordSet.getId(), fileHandle, csvDescriptor);
		final List<ColumnModel> schema = reconciled.getSchema();
		final List<Integer> columnsRequiredByJsonSchemaIndices = reconciled.getRequiredColumnIndices();

		if (schema == null || schema.isEmpty()) {
			throw new IllegalArgumentException("Cannot determine the schema from the CSV file, at least one column header must be present.");
		}

		// We can now read the CSV file again and reuse the PatchRowHandler.
		CSVReader csvReader = csvProvider.getCsvReader(fileHandle, csvDescriptor);
		SnapshotRowHandler rowHandler = getSnapshotRowHandler(snapshotStore, session, replica, schema, columnsRequiredByJsonSchemaIndices,
				fileProvider, user.getId(), validationSchemaId.orElse(null));
		
		try (csvReader; rowHandler) {

			// Skip the header
			csvReader.readNext();

			String[] csvRow;

			while ((csvRow = csvReader.readNext()) != null) {
				rowHandler.nextRow(new Row().setValues(Arrays.asList(csvRow)));
			}

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// For RecordSet sources, checkSourceAccess() already enforces READ+DOWNLOAD+UPDATE
		// on the source entity (and its benefactor), so no explicit benefactor IDs are needed.
		return new CreateGridHandlerResult().setGridSession(session).setGridReplica(replica)
				.setBenefactorIds(Collections.emptySet());
	}

	SnapshotRowHandler getSnapshotRowHandler(SnapshotStore snapshotStore, GridSession session, GridReplica replica,
											 List<ColumnModel> schema, List<Integer> requiredColumnIndices, FileProvider fileProvider,
											 Long createdByUserId, String schemaId) {
		return new SnapshotRowHandler(snapshotStore, session.getSessionId(), replica.getReplicaId(), schema, requiredColumnIndices,
				fileProvider, encoderProvider, createdByUserId, gridRowValidator, schemaId);
	}

}
