package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for TableEntities.
 * 
 * @author John
 *
 */
public class TableEntityMetadataProvider implements EntityValidator<TableEntity>, TypeSpecificDeleteProvider<TableEntity> {
	
	@Autowired
	TableRowManager tableRowManager;		

	@Override
	public void validateEntity(TableEntity entity, EntityEvent event) throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// For create/update/new version we need to bind the columns to the entity.
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			// Set the schema of the table.
			tableRowManager.setTableSchema(event.getUserInfo(), entity.getColumnIds(), entity.getId());
		}	
	}

	@Override
	public void entityDeleted(String deletedId) {
		tableRowManager.deleteTable(deletedId);
	}
}
