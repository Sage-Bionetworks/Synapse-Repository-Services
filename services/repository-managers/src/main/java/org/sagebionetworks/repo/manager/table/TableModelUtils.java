package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableEntity;

public class TableModelUtils {
	
	/**
	 * Validate the passed table model.
	 * @param toValidate
	 */
	public static void validateNewTableModel(TableEntity toValidate){
		if(toValidate == null) throw new IllegalArgumentException("TableEntity cannot be null");
		// Issue an ID for each column
		List<ColumnModel> models = toValidate.getColumns();
		if(models != null){
			for(int i=0; i<models.size();i++){
				ColumnModel mod = models.get(i);
				// Use the index as the ID for all new models
				mod.setId(""+i);	
			}
		}
	}
	
	/**
	 * Validate the new TableEntity meets all of the requirements.
	 * @param newEntity
	 * @param currentEntity
	 * @return
	 */
	public static boolean validateTableModelUpdate(TableEntity newEntity, TableEntity currentEntity){
		if(newEntity == null) throw new IllegalArgumentException("new TableEntity cannot be null");
		if(currentEntity == null) throw new IllegalArgumentException("current TableEntity cannot be null");
		// first map the IDs to the column model
		
		return true;
	}

}
