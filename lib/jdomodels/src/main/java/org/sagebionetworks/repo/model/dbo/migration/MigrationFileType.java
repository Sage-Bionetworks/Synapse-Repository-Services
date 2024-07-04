package org.sagebionetworks.repo.model.dbo.migration;

import org.sagebionetworks.util.ValidateArgument;

public enum MigrationFileType {

	JSON, XML;

	public static MigrationFileType fromFileName(String fileName) {
		ValidateArgument.required(fileName, "fileName");
		String[] split = fileName.split("\\.");
		if (split.length < 2) {
			throw new IllegalArgumentException(String.format("Unknown file type: '%s'", fileName));
		}
		return MigrationFileType.valueOf(split[split.length-1].toUpperCase());
	}
}
