package org.sagebionetworks.repo.manager.file.scanner;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

/**
 * Special {@link RowMapperSupplier} that can be used in a file handle scanner to extract file handles from
 * a serialized field given a function that operates on a deserialized object of type T
 *
 * @param <T> The type of the serialized object
 */
public class SerializedFieldRowMapperSupplier<T> implements RowMapperSupplier {
	
	private static final Logger LOG = LogManager.getLogger(SerializedFieldRowMapperSupplier.class);
	
	private Deserializer<T> deserializer;
	private FileHandleExtractor<T> fileHandlesExtractor;

	/**
	 * Builds a {@link RowMapperSupplier} used to extract the file handles from a serialized field
	 * 
	 * @param deserializer A deserializer for an instance of type T
	 * @param fileHandlesExtractor An extractor from a instance of type T into a set of file handles 
	 */
	public SerializedFieldRowMapperSupplier(Deserializer<T> deserializer, FileHandleExtractor<T> fileHandlesExtractor) {
		this.deserializer = deserializer;
		this.fileHandlesExtractor = fileHandlesExtractor;
	}

	@Override
	public RowMapper<ScannedFileHandleAssociation> getRowMapper(String objectIdColumnName, String serializedFieldColumnName) {
		return (ResultSet rs, int rowNum) -> {
			final Long objectId = rs.getLong(objectIdColumnName);
			
			final ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(objectId);
			
			final java.sql.Blob blob = rs.getBlob(serializedFieldColumnName);
			
			if (blob == null) {
				return association;
			}
			
			final byte[] serializedField = blob.getBytes(1, (int) blob.length());
			
			final T deserializedObject = deserializer.deserialize(serializedField);
			
			final Set<String> fileHandleIds = fileHandlesExtractor.extractAll(deserializedObject);
			
			if (fileHandleIds == null || fileHandleIds.isEmpty()) {
				return association;
			}

			return association.withFileHandleIds(mapFileHandleIds(fileHandleIds));
		};
	}
	
	private static Set<Long> mapFileHandleIds(Set<String> fileHandleIds) {
		return fileHandleIds.stream().map(idString -> {
			try {
				return Long.valueOf(idString);
			} catch (NumberFormatException e) {
				LOG.warn("Malformed file handle id: " + idString, e);
			}
			return null;
		})
		.filter(Objects::nonNull)
		.collect(Collectors.toSet());
	}
	
	/**
	 * A deserializer that given an array of bytes reconstruct an instance of type T
	 * 
	 * @param <T>
	 */
	@FunctionalInterface
	public static interface Deserializer<T> {
		
		T deserialize(byte[] bytes);
		
	}
	
	/**
	 * An extractor that given an instance of type T returns the set of file handle ids
	 * 
	 * @param <T>
	 */
	@FunctionalInterface
	public static interface FileHandleExtractor<T> {
		
		Set<String> extractAll(T deserialized);
	}

}
