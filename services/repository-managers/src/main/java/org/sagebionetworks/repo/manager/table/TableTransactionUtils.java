package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Common utilities for table transaction processing.
 * 
 * @author John
 *
 */
public class TableTransactionUtils {

	/**
	 * Validate the passed TableUpdateTransactionRequest request.
	 * 
	 * @param request
	 */
	public static void validateRequest(TableUpdateTransactionRequest request){
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityId(), "request.entityId");
		ValidateArgument.required(request.getChanges(), "request.changes");
		if(request.getChanges().isEmpty()){
			throw new IllegalArgumentException("Must include be at least one change.");
		}
		String tableId = request.getEntityId();
		for(TableUpdateRequest change: request.getChanges()){
			if(change.getEntityId() == null){
				change.setEntityId(tableId);
			}
			if(!tableId.equals(change.getEntityId())){
				throw new IllegalArgumentException("EntityId of TableUpdateRequest does not match the requested transaction entityId");
			}
		}
	}
}
