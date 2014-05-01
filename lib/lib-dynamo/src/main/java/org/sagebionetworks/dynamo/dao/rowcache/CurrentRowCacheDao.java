package org.sagebionetworks.dynamo.dao.rowcache;

import java.util.Map;

import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;

import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;

public interface CurrentRowCacheDao {
	boolean isEnabled();

	CurrentRowCacheStatus getLatestCurrentVersionNumber(String tableId);

	void setLatestCurrentVersionNumber(CurrentRowCacheStatus oldStatus, Long newLastCurrentVersion) throws ConditionalCheckFailedException;

	void putCurrentVersion(String tableId, Long rowId, Long versionNumber);

	void putCurrentVersions(String tableId, Map<Long, Long> rowsAndVersions);

	Long getCurrentVersion(String tableId, Long rowId);

	Map<Long, Long> getCurrentVersions(String tableId, Iterable<Long> rowIds);

	void deleteCurrentVersion(String tableId, Long rowId);

	void deleteCurrentVersions(String tableId, Iterable<Long> rowIds);

	void deleteCurrentTable(String tableId);

	void truncateAllData();
}
