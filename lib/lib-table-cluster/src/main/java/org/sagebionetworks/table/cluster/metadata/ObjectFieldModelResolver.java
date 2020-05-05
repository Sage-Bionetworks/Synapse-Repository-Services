package org.sagebionetworks.table.cluster.metadata;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectField;

/**
 * Service to resolve column models for the default replication {@link ObjectField}s.
 * 
 * @author Marco Marasca
 */
public interface ObjectFieldModelResolver {

	/**
	 * Given an object field returns the corresponding {@link ColumnModel}
	 * 
	 * @param field The field to translate
	 * @return The translated field
	 */
	ColumnModel getColumnModel(ObjectField field);
	
	/**
	 * @return All the column models mapped to {@link ObjectField}s
	 */
	List<ColumnModel> getAllColumnModels();
	
	/**
	 * Given a column model returns a matching ObjectField if the model can be matched one of the fields
	 * 
	 * @param columnModel The column model to match
	 * @return An optional containing the matching object field
	 */
	Optional<ObjectField> findMatch(ColumnModel columnModel);
	
}
