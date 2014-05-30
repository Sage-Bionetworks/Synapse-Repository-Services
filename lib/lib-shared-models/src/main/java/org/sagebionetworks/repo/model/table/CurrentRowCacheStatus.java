package org.sagebionetworks.repo.model.table;

public class CurrentRowCacheStatus {
	private Long tableId;
	private Long latestCachedVersionNumber = null;
	private Long recordVersion = null;

	public CurrentRowCacheStatus(Long tableId, Long latestVersionNumber, Long recordVersion) {
		this.tableId = tableId;
		this.latestCachedVersionNumber = latestVersionNumber;
		this.recordVersion = recordVersion;
	}

	public Long getTableId() {
		return tableId;
	}

	public Long getLatestCachedVersionNumber() {
		return latestCachedVersionNumber;
	}

	public Long getRecordVersion() {
		return recordVersion;
	}

	@Override
	public String toString() {
		return "CurrentRowCacheStatus [tableId=" + tableId + ", latestVersionNumber=" + latestCachedVersionNumber + ", recordVersion="
				+ recordVersion + "]";
	}

}
