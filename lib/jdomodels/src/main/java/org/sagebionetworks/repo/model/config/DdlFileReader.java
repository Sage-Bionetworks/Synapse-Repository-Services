package org.sagebionetworks.repo.model.config;

/**
 * Interface for reading DDL file content. Allows testing without real files.
 */
public interface DdlFileReader {

	/**
	 * Reads DDL content for the given file path.
	 *
	 * @param ddlFileName Path to the DDL file
	 * @return DDL content as a string, or null if not found
	 */
	String readDdl(String ddlFileName);
}
