package org.sagebionetworks.repo.web.service.table;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation of the TableServices.
 * 
 * @author John
 *
 */
public class TableServicesImpl implements TableServices {
	
	@Autowired
	UserManager userManager;
	@Autowired
	ColumnModelManager columnModelManager;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;

	@Override
	public ColumnModel createColumnModel(Long userId, ColumnModel columnModel) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.createColumnModel(user, columnModel);
	}

	@Override
	public ColumnModel getColumnModel(Long userId, String columnId) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		return columnModelManager.getColumnModel(user, columnId);
	}

	@Override
	public PaginatedColumnModels getColumnModelsForTableEntity(Long userId, String entityId) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		List<ColumnModel> models = getCurrentColumnsForTable(user, entityId);
		PaginatedColumnModels pcm = new PaginatedColumnModels();
		pcm.setResults(models);
		return pcm;
	}

	@Override
	public PaginatedColumnModels listColumnModels(Long userId, String prefix, Long limit, Long offset) throws DatastoreException, NotFoundException {
		UserInfo user = userManager.getUserInfo(userId);
		if(limit == null){
			limit = new Long(10);
		}
		if(offset == null){
			offset = new Long(0);
		}
		return columnModelManager.listColumnModels(user, prefix, limit, offset);
	}

	@Override
	public RowReferenceSet appendRows(Long userId, RowSet rows) throws DatastoreException, NotFoundException, IOException {
		if(rows == null) throw new IllegalArgumentException("Rows cannot be null");
		if(rows.getTableId() == null) throw new IllegalArgumentException("RowSet.tableId cannot be null");
		UserInfo user = userManager.getUserInfo(userId);
		List<ColumnModel> models = getCurrentColumnsForTable(user, rows.getTableId());
		return tableRowManager.appendRows(user, rows.getTableId(), models, rows);
	}

	@Override
	public RowReferenceSet deleteRows(Long userId, RowReferenceSet rowsToDelete) throws DatastoreException, NotFoundException, IOException {
		Validate.required(rowsToDelete, "rowsToDelete");
		Validate.required(rowsToDelete.getTableId(), "rowsToDelete.tableId");
		UserInfo user = userManager.getUserInfo(userId);
		List<ColumnModel> models = getCurrentColumnsForTable(user, rowsToDelete.getTableId());
		return tableRowManager.deleteRows(user, rowsToDelete.getTableId(), models, rowsToDelete);
	}
	
	private List<ColumnModel> getCurrentColumnsForTable(UserInfo user, String tableId) throws DatastoreException, NotFoundException{
		return columnModelManager.getColumnModelsForTable(user, tableId);
	}

	@Override
	public RowSet query(Long userId, Query query, boolean isConsisitent, boolean countOnly) throws NotFoundException, DatastoreException, TableUnavilableException {
		UserInfo user = userManager.getUserInfo(userId);
		return tableRowManager.query(user, query.getSql(), isConsisitent, countOnly);
	}
	
}
