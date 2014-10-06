package org.sagebionetworks.dynamo.dao.rowcache;

import java.util.Map;

import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.util.ProgressCallback;

import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;

public interface CurrentRowCacheDao {
	boolean isEnabled();

	CurrentRowCacheStatus getLatestCurrentVersionNumber(Long tableId);

	void setLatestCurrentVersionNumber(CurrentRowCacheStatus oldStatus, Long newLastCurrentVersion) throws ConditionalCheckFailedException;

	void putCurrentVersion(Long tableId, Long rowId, Long versionNumber);

	void putCurrentVersions(Long tableId, Map<Long, Long> rowsAndVersions, ProgressCallback<Long> progressCallback);

	Long getCurrentVersion(Long tableId, Long rowId);

	Map<Long, Long> getCurrentVersions(Long tableId, Iterable<Long> rowIds);

	Map<Long, Long> getCurrentVersions(Long tableId, long rowIdOffset, long limit);

	void deleteCurrentVersion(Long tableId, Long rowId);

	void deleteCurrentVersions(Long tableId, Iterable<Long> rowIds);

	void deleteCurrentTable(Long tableId);

	void truncateAllData();
}
