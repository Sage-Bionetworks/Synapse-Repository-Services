package org.sagebionetworks.repo.manager.grid.create;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.UploadPreviewBuilder;
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
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class RecordSetCreateGridHandler implements CreateGridHandler {

	private final GridDao gridDao;
	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;
	private final EntityAuthorizationManager authorizationManager;
	private final CsvFileHandleProvider csvProvider;
	private final JsonSchemaManager jsonSchemaManager;

	public RecordSetCreateGridHandler(GridDao gridDao, EntityManager entityManager, FileHandleManager fileHandleManager, EntityAuthorizationManager authorizationManager, CsvFileHandleProvider csvProvider, JsonSchemaManager jsonSchemaManager) {
		super();
		this.gridDao = gridDao;
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
		this.authorizationManager = authorizationManager;
		this.csvProvider = csvProvider;
		this.jsonSchemaManager = jsonSchemaManager;
	}

	@Override
	public boolean canCreate(CreateGridRequest request) {
		return request.getRecordSetId() != null;
	}

	@Override
	public CreateGridHandlerResult createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request,
			PatchStore patchStore) {
		String recordSetId = request.getRecordSetId();
		
		RecordSet recordSet = entityManager.getEntity(user, recordSetId, RecordSet.class);
		
		// Makes sure the user has download access
		authorizationManager.hasAccess(user, recordSet.getId(), ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();
		
		Optional<String> validationSchemaId = entityManager.findBoundSchema(recordSetId)
				.map(binding -> binding.getJsonSchemaVersionInfo().get$id());

		GridSession session = gridDao.createGridSession(new CreateGridSession().setUserId(user.getId())
				.setSourceId(recordSet.getId()).setSchemaId(validationSchemaId.orElse(null)).setOwner(request.getOwnerPrincipalId()));

		GridReplica replica = gridDao.createReplica(user.getId(), session.getSessionId(), false, EventSource.INTERNAL);

		// We already checked that the user has download access
		FileHandle fileHandle = fileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId());

		CsvTableDescriptor csvDescriptor = recordSet.getCsvDescriptor();

		if (csvDescriptor == null) {
			csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		}

		// In order to emit patches using the PatchRowHandler we need a starting schema,
		// this is needed so that
		// the values in a row are emitted with some sensible data types. Additionally,
		// we split into multiple
		// patches according to the max size of each row.
		//
		// In order to determine the correct schema and size we first scan the CSV file
		// reusing the UploadPreviewBuilder
		// that allows to compute a suggested schema from a CSV file.
		List<ColumnModel> schema = getSchemaFromCsv(fileHandle, csvDescriptor);

		final List<String> columnsRequiredByJsonSchema = validationSchemaId
				.map(jsonSchemaManager::getValidationSchema)
				.map(JsonSchema::getRequired)
				.orElse(new ArrayList<>());

		final Map<String, Integer> columnNameToIndex = new HashMap<>();
		for (int i = 0; i < schema.size(); i++) {
			columnNameToIndex.put(schema.get(i).getName(), i);
		}
		final List<Integer> columnsRequiredByJsonSchemaIndices = columnsRequiredByJsonSchema.stream()
				.map(columnNameToIndex::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());



		if (schema == null || schema.isEmpty()) {
			throw new IllegalArgumentException("Cannot determine the schema from the CSV file, at least one column header must be present.");
		}

		Long maxBytesPerRow = (long) TableModelUtils.calculateMaxRowSize(schema);

		// We can now read the CSV file again and reuse the PatchRowHandler.
		CSVReader csvReader = csvProvider.getCsvReader(fileHandle, csvDescriptor);
		PatchRowHandler rowHandler = getPatchRowHandler(patchStore, session, replica, schema, maxBytesPerRow, columnsRequiredByJsonSchemaIndices);
		
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

		return new CreateGridHandlerResult().setGridSession(session).setGridReplica(replica);
	}

	List<ColumnModel> getSchemaFromCsv(FileHandle fileHandle, CsvTableDescriptor csvDescriptor) {
		try (CSVReader csvReader = csvProvider.getCsvReader(fileHandle, csvDescriptor)) {

			// Reuse the CSV preview builder to extract the schema
			UploadToTablePreviewRequest request = new UploadToTablePreviewRequest().setCsvTableDescriptor(csvDescriptor)
					// We do a full scan so that the row size is accurate
					.setDoFullFileScan(true);

			return new UploadPreviewBuilder(csvReader, request).buildResult().getSuggestedColumns();

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	PatchRowHandler getPatchRowHandler(PatchStore patchStore, GridSession session, GridReplica replica,
			List<ColumnModel> schema, Long maxBytesPerRow, List<Integer> requiredColumnIndices) {
		return new PatchRowHandler(patchStore, session.getSessionId(), replica.getReplicaId(), schema, maxBytesPerRow, requiredColumnIndices);
	}

}
