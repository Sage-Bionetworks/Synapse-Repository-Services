package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.RowMapper;

/**
 * {@link RowMapperSupplier} for file handle ids held inside a serialized field that the database
 * returns as a string, such as a JSON column. {@link SerializedFieldRowMapperSupplier} covers fields
 * stored as a blob.
 */
public class StringSerializedFieldRowMapperSupplier implements RowMapperSupplier {

	private final Function<String, Set<Long>> fileHandleIdsExtractor;

	/**
	 * @param fileHandleIdsExtractor Given the serialized field of a row, possibly null, returns the file
	 *                               handle ids it holds. Must never return null.
	 */
	public StringSerializedFieldRowMapperSupplier(Function<String, Set<Long>> fileHandleIdsExtractor) {
		ValidateArgument.required(fileHandleIdsExtractor, "fileHandleIdsExtractor");
		this.fileHandleIdsExtractor = fileHandleIdsExtractor;
	}

	@Override
	public RowMapper<ScannedFileHandleAssociation> getRowMapper(String objectIdColumnName,
			List<String> serializedFieldColumnNames) {
		ValidateArgument.requirement(serializedFieldColumnNames.size() == 1,
				"Exactly one serialized field column is supported.");
		String serializedFieldColumnName = serializedFieldColumnNames.get(0);
		return (ResultSet rs, int rowNum) -> new ScannedFileHandleAssociation(rs.getLong(objectIdColumnName))
				.withFileHandleIds(fileHandleIdsExtractor.apply(rs.getString(serializedFieldColumnName)));
	}

}
