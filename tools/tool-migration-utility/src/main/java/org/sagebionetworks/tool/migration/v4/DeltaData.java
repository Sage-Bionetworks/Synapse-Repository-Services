package org.sagebionetworks.tool.migration.v4;

import java.io.File;

import org.sagebionetworks.repo.model.migration.MigrationType;

public class DeltaData {
	File createTemp;
	File updateTemp;
	File deleteTemp;
	MigrationType type;
	DeltaCounts counts;
	
	/**
	 * Delta data for a single type.
	 * 
	 * @param type
	 * @param createTemp
	 * @param updateTemp
	 * @param deleteTemp
	 */
	public DeltaData(MigrationType type, File createTemp, File updateTemp,
			File deleteTemp, DeltaCounts counts) {
		super();
		this.type = type;
		this.createTemp = createTemp;
		this.updateTemp = updateTemp;
		this.deleteTemp = deleteTemp;
		this.counts = counts;
	}
	public File getCreateTemp() {
		return createTemp;
	}
	public File getUpdateTemp() {
		return updateTemp;
	}
	public File getDeleteTemp() {
		return deleteTemp;
	}
	public MigrationType getType() {
		return type;
	}
	public DeltaCounts getCounts() {
		return counts;
	}
	
}
