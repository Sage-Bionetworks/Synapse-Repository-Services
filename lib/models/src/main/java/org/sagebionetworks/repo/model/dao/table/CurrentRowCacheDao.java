package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;

import org.sagebionetworks.util.ProgressCallback;

public interface CurrentRowCacheDao {
	boolean isEnabled();

	long getLatestCurrentVersionNumber(Long tableId);

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
