package org.sagebionetworks.tool.migration.v4.Delta;

import java.util.List;

import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.tool.migration.v4.RangeMetadataIterator;

/**
 * Container for list of ranges where there is a difference between prod and staging
 *
 */
public class DeltaRanges {
	
	private MigrationType migrationType;
	private List<IdRange> insRanges;
	private List<IdRange> updRanges;
	private List<IdRange> delRanges;
	
	public DeltaRanges() {
	}

	public MigrationType getMigrationType() {
		return migrationType;
	}

	public void setMigrationType(MigrationType migrationType) {
		this.migrationType = migrationType;
	}

	public List<IdRange> getInsRanges() {
		return insRanges;
	}

	public void setInsRanges(List<IdRange> insRanges) {
		this.insRanges = insRanges;
	}

	public List<IdRange> getUpdRanges() {
		return updRanges;
	}

	public void setUpdRanges(List<IdRange> updRanges) {
		this.updRanges = updRanges;
	}

	public List<IdRange> getDelRanges() {
		return delRanges;
	}

	public void setDelRanges(List<IdRange> delRanges) {
		this.delRanges = delRanges;
	}

}
