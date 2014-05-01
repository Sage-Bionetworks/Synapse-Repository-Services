package org.sagebionetworks.repo.model.table;

public class CurrentRowCacheStatus {
	private String tableId;
	private Long latestCachedVersionNumber = null;
	private Long recordVersion = null;

	public CurrentRowCacheStatus(String tableId, Long latestVersionNumber, Long recordVersion) {
		this.tableId = tableId;
		this.latestCachedVersionNumber = latestVersionNumber;
		this.recordVersion = recordVersion;
	}

	public String getTableId() {
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
