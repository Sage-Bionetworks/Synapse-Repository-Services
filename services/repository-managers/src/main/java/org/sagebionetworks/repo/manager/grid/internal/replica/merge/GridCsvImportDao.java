package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.Iterator;

public interface GridCsvImportDao {
	
	void streamToCsvTempTable(String sessionId, DataStream dataIterator, ColumnMapping[] columnMapping);
	
	void streamToGridTempTable(String sessionId, DataStream dataIterator, ColumnMapping[] columnMapping);
	
	Iterator<Object[]> getCsvTempTableIterator(String sessionId);
	
	Iterator<Object[]> getGridTempTableIterator(String sessionId);
	
	Iterator<JoinedRow> getJoinedTempTableIterator(String sessionId, ColumnMapping[] columnMapping);
	
	void dropTemporaryTables(String sessionId);
}
