package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScannerTestUtils;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableFileHandleScannerAutowireTest {

	@Autowired
	private TableEntityManager tableManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private ColumnModelManager modelManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private TableTransactionDao tableTransactionDao;
	
	@Autowired
	private TableRowTruthDAO tableTruthDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private TableFileHandleScanner scanner;
	
	private UserInfo user;
	
	private Long tableWithNoSchema;
	private Long tableWithNoFiles;
	private Long tableWithFiles;
	
	private List<String> fileHandlesIds;
	
	private IdRange idRange;
	
	@BeforeEach
	public void before() throws Exception {
		
		tableTruthDao.truncateAllRowData();
		
		fileHandlesIds = new ArrayList<>();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		fileHandlesIds.add(utils.generateFileHandle(user));
		fileHandlesIds.add(utils.generateFileHandle(user));
		fileHandlesIds.add(utils.generateFileHandle(user));
		
		tableWithNoSchema = generateTable(null);
		tableWithNoFiles = generateTable(generateSchema(ColumnType.STRING, ColumnType.STRING));
		tableWithFiles = generateTable(generateSchema(ColumnType.STRING, ColumnType.FILEHANDLEID));
		
		addTableData(tableWithNoFiles.toString(), 3);

		// Single row
		addTableData(tableWithFiles.toString(), 1, fileHandlesIds.get(0));
		
		// Another batch with the remaining ids
		String[] remainingIds = fileHandlesIds.subList(1, fileHandlesIds.size()).toArray(new String[fileHandlesIds.size() - 1]);		

		addTableData(tableWithFiles.toString(), remainingIds.length, remainingIds);
		
		// No transaction for the first table (e.g. no schema)
		idRange = new IdRange(tableWithNoFiles, tableWithFiles);
		
	}
	
	@AfterEach
	public void after() {
		tableTruthDao.truncateAllRowData();
		
		tableTransactionDao.deleteTable(tableWithNoSchema.toString());
		tableTransactionDao.deleteTable(tableWithFiles.toString());
		tableTransactionDao.deleteTable(tableWithNoFiles.toString());
		
		entityManager.deleteEntity(user, tableWithNoSchema.toString());
		entityManager.deleteEntity(user, tableWithNoFiles.toString());
		entityManager.deleteEntity(user, tableWithFiles.toString());
		
		fileHandleDao.truncateTable();
		
	}
	
	@Test
	public void testScanner() {
		
		// Call under test
		assertEquals(idRange, scanner.getIdRange());
		
		List<ScannedFileHandleAssociation> expected = new ArrayList<>();

		// No file in the schema, but the first change is the column change (schema)
		expected.add(new ScannedFileHandleAssociation(tableWithNoFiles));
		// No file in the schema for this change (row change but no file handles)
		expected.add(new ScannedFileHandleAssociation(tableWithNoFiles).withFileHandleIds(Collections.emptySet()));
		
		// File in the schema, but the first change is the column change (schema)
		expected.add(new ScannedFileHandleAssociation(tableWithFiles));
		// The first change has the first file handle
		expected.add(new ScannedFileHandleAssociation(tableWithFiles, Long.valueOf(fileHandlesIds.get(0))));
		// The second change has the remaining file handles
		expected.add(new ScannedFileHandleAssociation(tableWithFiles).withFileHandleIds(
				fileHandlesIds.subList(1, fileHandlesIds.size()).stream().map(Long::valueOf).collect(Collectors.toSet()))
		);
		
		// Call under test, consume the whole iterable
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(idRange).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	private void addTableData(String tableId, int rowNumber, String ...fileHandleIds) throws DatastoreException, NotFoundException, IOException {
		
		List<ColumnModel> currentSchema = modelManager.getColumnModelsForTable(user, tableId);
		
		RowSet rowSet = new RowSet();
		
		rowSet.setTableId(tableId);
		rowSet.setRows(TableModelTestUtils.createRows(currentSchema, rowNumber, Arrays.asList(fileHandleIds)));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(currentSchema));
		
		long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		
		tableManager.appendRows(user, tableId, rowSet, null, transactionId);
	}
	
	private List<ColumnModel> generateSchema(ColumnType ...columnTypes) {
		
		List<ColumnModel> schema = Arrays.stream(columnTypes).map( type -> {
			ColumnModel model = new ColumnModel();
			model.setColumnType(type);
			model.setName(UUID.randomUUID().toString());
			return model;
		}).collect(Collectors.toList());
		
		return modelManager.createColumnModels(user, schema);
		
	}
	
	private Long generateTable(List<ColumnModel> model) {
		
		List<String> columnIds = null;
		
		if (model != null) {
			columnIds = model.stream().map(ColumnModel::getId).collect(Collectors.toList());
		}
		
		Table table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(columnIds);
		
		String tableId = entityManager.createEntity(user, table, null);
		
		if (columnIds != null && !columnIds.isEmpty()) {
			tableManager.setTableSchema(user, columnIds, tableId);
		}
		
		return KeyFactory.stringToKey(tableId);
	}

	
	
}
