package org.sagebionetworks.repo.manager.table.metadata;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;

public interface DefaultColumnModelMapper {

	/**
	 * Maps the given {@link DefaultColumnModel} to a list of {@link ColumnModel}
	 * stored in the database
	 * 
	 * @param defaultColumns The {@link DefaultColumnModel}
	 * @return The list of {@link ColumnModel} that map to the given
	 *         {@link DefaultColumnModel} as they are stored in the database
	 */
	List<ColumnModel> map(DefaultColumnModel defaultColumns);
	
	// Used for testing
	
	/**
	 * Returns the column models mapped to the given object fields in the database
	 * 
	 * @param objectType The object type
	 * @param fields The object fields
	 * @return The column models stored in the database for the given object fields
	 */
	List<ColumnModel> getColumnModels(ViewObjectType objectType, ObjectField ...fields);

}
