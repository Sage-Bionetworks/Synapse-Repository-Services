package org.sagebionetworks.repo.manager.file.scanner;

import org.springframework.jdbc.core.RowMapper;

/**
 * Functional interface that provides a row mapper and provides access to the name of the object and
 * file handle ids columns
 */
@FunctionalInterface
public interface RowMapperSupplier {

	/**
	 * @param objectIdColumnName The name of the object id column
	 * @param fileHandleIdColumnName The name of the column where the file handle id(s) are stored
	 * @return A row mapper for a {@link ScannedFileHandleAssociation}
	 */
	RowMapper<ScannedFileHandleAssociation> getRowMapper(String objectIdColumnName, String fileHandleIdColumnName);

}
