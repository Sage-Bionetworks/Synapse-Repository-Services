package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.sagebionetworks.grid.db.GridTransaction;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.grid.CsvSchemaReconciler;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class GridCsvImporterImpl implements GridCsvImporter {
	
	private final GridManager gridManager;
	private final GridReplicaViewManager gridViewManager;
	private final GridReplicaSupport replicaSupport;
	private final GridCsvImportDao importDao;
	private final CsvFileHandleProvider csvProvider;
	private final JoinedRowChangePublisher changePublisher;
	private final JsonSchemaManager jsonSchemaManager;

	public GridCsvImporterImpl(GridCsvImportDao importDao, GridManager gridManager, GridReplicaViewManager gridViewManager, GridReplicaSupport replicaSupport, CsvFileHandleProvider csvProvider, JoinedRowChangePublisher changePublisher, JsonSchemaManager jsonSchemaManager) {
		this.importDao = importDao;
		this.gridManager = gridManager;
		this.replicaSupport = replicaSupport;
		this.gridViewManager = gridViewManager;
		this.csvProvider = csvProvider;
		this.changePublisher = changePublisher;
		this.jsonSchemaManager = jsonSchemaManager;
	}
	
	@Override
	@GridTransaction(readOnly = false)
	public GridCsvImportResponse importCsv(UserInfo user, GridCsvImportRequest request, AsyncJobProgressCallback jobCallback) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		ValidateArgument.required(request.getFileHandleId(), "request.fileHandleId");
		ValidateArgument.required(request.getCsvDescriptor(), "request.csvDescriptor");
		ValidateArgument.required(request.getSchema(), "request.schema");
		ValidateArgument.requirement(Boolean.TRUE.equals(request.getCsvDescriptor().getIsFirstLineHeader()), "The request.csvDescriptor.isFirstLineHeader must be true.");
	
		GridSession gridSession = gridManager.getGridSession(user, request.getSessionId());

		Optional.ofNullable(gridSession.getGridJsonSchema$Id())
				.map(jsonSchemaManager::getValidationSchema)
				.ifPresent(vs -> CsvSchemaReconciler.reconcile(request.getSchema(), vs));

		GridHeader gridHeader = replicaSupport.getGridHeaderOrThrow(gridSession);
		
		// Publish the imported changes under a replica owned by the importing user, so the
		// imported cells carry user attribution (PLFM-9880)
		GridConnectionInfo publisherConnInfo = gridManager.getOrCreateUserConnection(gridSession.getSessionId(),
				user, EventSource.IMPORT);
		
		List<String> upsertKey = replicaSupport.getRecordSetOrThrow(user, gridSession).getUpsertKey();

		ColumnMapping[] columnMapping;
		
		try {
			// First create a temporary table containing the CSV data
			try (CSVReader csvReader = csvProvider.getCsvReader(user, request.getFileHandleId(), request.getCsvDescriptor())) {
				
				// We need to make sure that the schema is sorted according to the potential CSV header
				validateHeader(csvReader, request.getSchema());
				
				// Computes the driving column mapping
				columnMapping = getColumnMapping(upsertKey, request.getSchema(), gridHeader.getOrderedColumns());
				
				importDao.streamToCsvTempTable(gridSession.getSessionId(), new CsvDataStream(csvReader, columnMapping), columnMapping);
			} catch(IllegalArgumentException e) {
				throw e;
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
			
			Iterator<RowView> gridDataIterator = gridViewManager.getQueryIterator(gridHeader, Collections.emptyList());
			
			// Now create a temporary table containing the grid data
			importDao.streamToGridTempTable(gridSession.getSessionId(), new GridDataStream(gridDataIterator, columnMapping), columnMapping);
			
			// Now join the two temporary tables to determine which rows are new and which rows are updates
			Iterator<JoinedRow> joinResult = importDao.getJoinedTempTableIterator(gridSession.getSessionId(), columnMapping);
			
			return changePublisher.processJoinedRows(gridHeader, publisherConnInfo, joinResult, columnMapping);
		
		} finally {
			importDao.dropTemporaryTables(gridSession.getSessionId());
		}
	}
	
	void validateHeader(CSVReader reader, List<ColumnModel> schema) throws IOException {		
		String[] header = reader.readNext();
		
		ValidateArgument.requirement(header != null, "The CSV file cannot be empty.");
		ValidateArgument.requirement(header.length == schema.size(), "The CSV header does not match the schema size.");

		for (int i = 0; i < header.length; i++) {
			String columnName = schema.get(i).getName();
			ValidateArgument.requirement(columnName.equals(header[i]), "The CSV header column \"" + header[i] + "\" does not match the schema column \"" + columnName + "\" at index: " + i);
		}
	}
	
	// Computes an ordered mapping by upsert key first and then the rest of the columns of the CSV that exist in the grid.
	ColumnMapping[] getColumnMapping(List<String> upsertKey, List<ColumnModel> csvSchema, List<Column> gridSchema) throws IOException {
		List<ColumnMapping> columnMapping = new ArrayList<>();
		
		Map<String, Integer> csvColumnIndex = new HashMap<>(csvSchema.size());
		
		IntStream.range(0, csvSchema.size())
			.forEach(i -> csvColumnIndex.put(csvSchema.get(i).getName(), i));
		
		Map<String, Integer> gridColumnIndex = new HashMap<>();
		
		IntStream.range(0, gridSchema.size())
			.forEach(i -> gridColumnIndex.put(gridSchema.get(i).getName(), i));
			
		// We first map by the upsert key order
		for (int i = 0; i < upsertKey.size(); i++) {
			String columnName = upsertKey.get(i);
			
			int csvIndex = csvColumnIndex.getOrDefault(columnName, -1);
			
			ValidateArgument.requirement(csvIndex >= 0, "The upsert key column \"" + columnName + "\" does not exist in the CSV schema.");
			
			int gridIndex = gridColumnIndex.getOrDefault(columnName, -1);
			
			ValidateArgument.requirement(gridIndex >= 0, "The upsert key column \"" + columnName + "\" does not exist in the grid schema.");
			
			columnMapping.add(new ColumnMapping(columnName, csvSchema.get(csvIndex).getColumnType(), csvIndex, gridIndex, true));
		}
		
		// Now maps the rest of the columns
		for (int csvIndex = 0; csvIndex < csvSchema.size(); csvIndex++) {
			String columnName = csvSchema.get(csvIndex).getName();
			
			if (upsertKey.contains(columnName)) {
				continue;
			}
			
			int gridIndex = gridColumnIndex.getOrDefault(columnName, -1);
			
			// We ignore columns that do not exist in the grid
			if (gridIndex < 0) {
				continue;
			}
			
			columnMapping.add(new ColumnMapping(columnName, csvSchema.get(csvIndex).getColumnType(), csvIndex, gridIndex, false));
		}
		
		return columnMapping.toArray(new ColumnMapping[0]);
	}
	
}
