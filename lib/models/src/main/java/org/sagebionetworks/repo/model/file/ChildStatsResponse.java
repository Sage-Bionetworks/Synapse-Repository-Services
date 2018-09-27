package org.sagebionetworks.repo.model.file;

/**
 * Response to get statistics for a parentId and types.
 *
 */
public class ChildStatsResponse {
	
	private Long totalChildCount;
	private Long sumFileSizesBytes;

	public Long getTotalChildCount() {
		return totalChildCount;
	}

	public Long getSumFileSizesBytes() {
		return sumFileSizesBytes;
	}

	public ChildStatsResponse withTotalChildCount(Long totalChildCount) {
		this.totalChildCount = totalChildCount;
		return this;
	}

	public ChildStatsResponse withSumFileSizesBytes(Long sumFileSizesBytes) {
		this.sumFileSizesBytes = sumFileSizesBytes;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sumFileSizesBytes == null) ? 0 : sumFileSizesBytes.hashCode());
		result = prime * result + ((totalChildCount == null) ? 0 : totalChildCount.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChildStatsResponse other = (ChildStatsResponse) obj;
		if (sumFileSizesBytes == null) {
			if (other.sumFileSizesBytes != null)
				return false;
		} else if (!sumFileSizesBytes.equals(other.sumFileSizesBytes))
			return false;
		if (totalChildCount == null) {
			if (other.totalChildCount != null)
				return false;
		} else if (!totalChildCount.equals(other.totalChildCount))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ParentStatsResponse [totalChildCount=" + totalChildCount + ", sumFileSizesBytes=" + sumFileSizesBytes
				+ "]";
	}

}
