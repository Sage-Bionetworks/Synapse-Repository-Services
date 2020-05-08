package org.sagebionetworks.table.cluster.metadata;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;

/**
 * Provides {@link ColumnType} mappings for specific {@link ObjectField} that
 * depend on the type of object being replicated
 * 
 * @author Marco Marasca
 */
public interface ObjectFieldTypeMapper {
	
	/**
	 * @return The object type this mapper applies to
	 */
	ViewObjectType getObjectType();

	/**
	 * @return The {@link ColumnType} mapping for the id of the object
	 */
	ColumnType getIdColumnType();

	/**
	 * @return The {@link ColumnType} mapping for the parent id of the object
	 */
	ColumnType getParentIdColumnType();

	/**
	 * @return The {@link ColumnType} mapping for the benefactor id of the object
	 */
	ColumnType getBenefactorIdColumnType();

}
