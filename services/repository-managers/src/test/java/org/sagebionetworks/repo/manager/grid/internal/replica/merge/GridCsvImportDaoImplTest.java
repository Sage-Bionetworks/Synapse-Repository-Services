package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridCsvImportDaoImplTest {

	@Autowired
	private GridReplicaViewManager gridViewManager;
	@Autowired
	private GridIndexManager gridIndexManger;
	@Autowired
	private GridCsvImportDao importDao;
	
	private String sessionId = GridUtils.gridSessionIdAsString(123L);
	private Long replicaId = 987L;
	
	int gridRowCount = 500;
	int csvRowCount = 750;
	
	private List<Row> gridRows;
	private String csvContent;
	private ColumnMapping[] columnMapping;
	
	@BeforeEach
	public void before() {
		gridIndexManger.truncateAll();
		
		gridRows = new ArrayList<>(gridRowCount);
		
		for (int i = 0; i < gridRowCount; i++) {
			gridRows.add(new Row().setValues(List.of(
				String.valueOf(i%10), 			// a
				String.valueOf(i), 				// b
				"foo" + i, 						// c	
				String.valueOf(i%2 == 0) 		// d
			)));
		}
		
		StringBuilder csv = new StringBuilder();
		
		for (int i=0; i < csvRowCount; i++) {
			csv
				.append(i).append(",") 						// b
				.append(i%5).append(",") 					// a, note that the grid uses mod 10, so we do not match half the upserts
				.append(false).append(",")					// d
				.append("bar" + i).append(",")				// c
				.append("extra" + i)						// e, this is not in the grid
				.append(System.lineSeparator());
		}

		this.csvContent = csv.toString();
		
		columnMapping = new ColumnMapping[] {
			new ColumnMapping("b", ColumnType.INTEGER, 0, 1, true),
			new ColumnMapping("a", ColumnType.INTEGER, 1, 0, true),
			new ColumnMapping("d", ColumnType.BOOLEAN, 2, 3, false),
			new ColumnMapping("c", ColumnType.STRING, 3, 2, false),
		};
	}
	
	@AfterEach
	public void after() {
		gridIndexManger.truncateAll();
	}
	
	@Test
	public void testImportAndJoin() throws IOException {
		
		writeGridData();
		
		GridHeader gridHeader = gridViewManager.readHeader(sessionId, replicaId).orElseThrow();
		
		long start = System.currentTimeMillis();
		
		Iterator<RowView> rowView = gridViewManager.getQueryIterator(gridHeader, Collections.emptyList());
		
		DataStream gridDataStream = new GridDataStream(rowView, columnMapping);
		
		importDao.streamToGridTempTable(sessionId, gridDataStream, columnMapping);
		
		System.out.println("Grid import took: " + (System.currentTimeMillis() - start) + "ms");
		
		Iterator<Object[]> it = importDao.getGridTempTableIterator(sessionId);
		
		long rowId = 0;
		Long rowVecId = 22L;
		
		while (it.hasNext()) {
			Object[] row = it.next();
			// This is the extra column with information from the grid (serialized vector id)
			String extraData = "[[" + replicaId + "," + rowVecId + "]]";
			// Note that the data has been reordered by the upsert key (b,a) and only contains the upsert keys plus the extra data column
			assertArrayEquals(new Object[] { rowId, rowId % 10, extraData }, row);
			rowId++;
			rowVecId+=9;
		}
		
		// Now import the CSV data (contains a subset of the grid data, with additional columns and different order)
		
		start = System.currentTimeMillis();
		
		CSVReader reader = CSVUtils.createCSVReader(new StringReader(csvContent), null, null);
		CsvDataStream csvDataStream = new CsvDataStream(reader, columnMapping);
		
		try (reader) {
			importDao.streamToCsvTempTable(sessionId, csvDataStream, columnMapping);
		}

		System.out.println("CSV import took: " + (System.currentTimeMillis() - start) + "ms");
		
		it = importDao.getCsvTempTableIterator(sessionId);
		rowId = 0;
		
		while (it.hasNext()) {
			String expectedCsvJson = "[false,\"bar" + rowId + "\"]";
			
			assertArrayEquals(
				// Note that column e is omitted as not present in the grid and the columns that are not
				// the upsert key are stored in a JSON array
				new Object[] { rowId, rowId % 5, expectedCsvJson },
				it.next()
			);
			
			rowId++;
		}
	
		start = System.currentTimeMillis();
		
		Iterator<JoinedRow> joinIt = importDao.getJoinedTempTableIterator(sessionId, columnMapping);
		
		int notMatchedCount = 0;
		
		while (joinIt.hasNext()) {
			// The join is in upsert key reverse order
			rowId--;
			
			String expectedCsvData = "[" + rowId + "," + rowId % 5 + "," + false + ",\"" + "bar" + rowId + "\"]";
			
			LogicalTimestamp expectedGridRowVecId = null;
			
			if (rowId < gridRowCount) {
				// We enter the grid data range, adjust the vec seq accordingly
				rowVecId-=9;
				
				if (rowId % 10 < 5) {
					// Only half the rows in the grid match the upsert key in the csv (mod % 10 vs mod % 5)
					expectedGridRowVecId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(rowVecId);
				}
			}
			
			if (expectedGridRowVecId == null) {
				notMatchedCount++;
			}
			
			JoinedRow next = joinIt.next();
			
			assertEquals(expectedCsvData, next.getCsvData().toString());
			assertEquals(expectedGridRowVecId, next.getGridRowVecId());
		}
		
		System.out.println("Join took: " + (System.currentTimeMillis() - start) + "ms");
		
		// The CSV has only 50% of the rows in common with the grid
		assertEquals(gridRowCount/2 + (csvRowCount - gridRowCount), notMatchedCount);
		
		importDao.dropTemporaryTables(sessionId);
	}
	
	void writeGridData() throws IOException {
		long start = System.currentTimeMillis();
		
		List<ColumnModel> schema = List.of(
			new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("b").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("c").setColumnType(ColumnType.STRING),
			new ColumnModel().setName("d").setColumnType(ColumnType.BOOLEAN)
		);
		
		PatchRowHandler patchRowHandler = new PatchRowHandler((s, pid, body) -> {
			gridIndexManger.applyPatch(sessionId, pid.getReplicaId(), PatchCompactSerializable.deserialize(new JSONArray(body)));
			return true;
		}, sessionId, replicaId, schema, 100L);
		
		try (patchRowHandler) {
			gridRows.stream().forEach(r -> {
				patchRowHandler.nextRow(r);
			});
		}
		
		System.out.println("Grid storage took: " + (System.currentTimeMillis() - start) + "ms");
	}
		

}
