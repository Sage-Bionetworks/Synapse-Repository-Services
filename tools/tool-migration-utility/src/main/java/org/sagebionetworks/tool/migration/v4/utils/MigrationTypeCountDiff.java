package org.sagebionetworks.tool.migration.v4.utils;

import org.sagebionetworks.repo.model.migration.MigrationType;

public class MigrationTypeCountDiff {
	private MigrationType type;
	private Long sourceCount;
	private Long destinationCount;
	
	public MigrationTypeCountDiff(MigrationType type, Long srcCount, Long destCount) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null.");
		}
		this.type = type;
		this.sourceCount = srcCount;
		this.destinationCount = destCount;
	}
	
	public MigrationType getType() {
		return type;
	}
	public void setType(MigrationType type) {
		this.type = type;
	}
	public Long getSourceCount() {
		return sourceCount;
	}
	public void setSourceCount(Long sourceCount) {
		this.sourceCount = sourceCount;
	}
	public Long getDestinationCount() {
		return destinationCount;
	}
	public void setDestinationCount(Long destinationCount) {
		this.destinationCount = destinationCount;
	}
	
	public Long getDelta() {
		if ((this.sourceCount == null) || (this.destinationCount == null)) {
			return null;
		}
		return (this.getDestinationCount() - this.getSourceCount());
	}
}
