package org.sagebionetworks.repo.manager.table;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnChangeDetails;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * Basic implementation of the ColumnModelManager.
 * 
 * @author John
 *
 */
public class ColumnModelManagerImpl implements ColumnModelManager {

	public static final String COLUMN_TYPE_ERROR_TEMPLATE = "A %1$s column cannot be changed to %2$s";
	/**
	 * This is the maximum number of bytes for a single row in MySQL.
	 * This determines the maximum schema size for a table.
	 */
	public static final int MY_SQL_MAX_BYTES_PER_ROW = 64000;
	public static final int MY_SQL_MAX_COLUMNS_PER_TABLE = 4095;
	
	@Autowired
	ColumnModelDAO columnModelDao;
	
	@Autowired
	AuthorizationManager authorizationManager;

	@Override
	public PaginatedColumnModels listColumnModels(UserInfo user, String namePrefix, long limit, long offset) {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		validateLimitOffset(limit, offset);
		// First call is to list the columns.
		List<ColumnModel> list = columnModelDao.listColumnModels(namePrefix, limit, offset);
		// second to get the total number of results with this prefix
		long totalNumberOfResults = columnModelDao.listColumnModelsCount(namePrefix);
		PaginatedColumnModels pcm = new PaginatedColumnModels();
		pcm.setResults(list);
		pcm.setTotalNumberOfResults(totalNumberOfResults);
		return pcm;
	}

	private void validateLimitOffset(long limit, long offset) {
		if(limit < 0) throw new IllegalArgumentException("Limit cannot be less than zero");
		if(limit > 100) throw new IllegalArgumentException("Limit cannot be greater than 100");
		if(offset < 0) throw new IllegalArgumentException("Offset cannot be less than zero");
	}
	
	@WriteTransaction
	@Override
	public ColumnModel createColumnModel(UserInfo user, ColumnModel columnModel) throws UnauthorizedException, DatastoreException, NotFoundException{
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		// Must login to create a column model.
		if(authorizationManager.isAnonymousUser(user)){
			throw new UnauthorizedException("You must login to create a ColumnModel");
		}
		validateColumnModel(columnModel);
		// Pass it along to the DAO.
		return columnModelDao.createColumnModel(columnModel);
	}
	
	@WriteTransaction
	@Override
	public List<ColumnModel> createColumnModels(UserInfo user, List<ColumnModel> columnModels) throws DatastoreException, NotFoundException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null");
		// Must login to create a column model.
		if (authorizationManager.isAnonymousUser(user)) {
			throw new UnauthorizedException("You must login to create a ColumnModel");
		}
		// first quickly check for naming errors
		for (ColumnModel columnModel : columnModels) {
			validateColumnModel(columnModel);
		}
		// the create all column models
		List<ColumnModel> results = Lists.newArrayListWithCapacity(columnModels.size());
		for (ColumnModel columnModel : columnModels) {
			// Pass it along to the DAO.
			results.add(columnModelDao.createColumnModel(columnModel));
		}
		return results;
	}

	private void validateColumnModel(ColumnModel columnModel) {
		checkColumnNaming(columnModel);
		validateFacetType(columnModel);
	}
	
	private void checkColumnNaming(ColumnModel columnModel) {
		// Validate the name
		if (TableConstants.isReservedColumnName(columnModel.getName())) {
			throw new IllegalArgumentException("The column name: " + columnModel.getName() + " is a system reserved column name.");
		}
		// Is the name a key word?
		if (TableConstants.isKeyWord(columnModel.getName())) {
			throw new IllegalArgumentException("The name: " + columnModel.getName()
					+ " is a SQL key word and cannot be used as a column name.");
		}
	}
	
	void validateFacetType(ColumnModel columnModel){
		//validate the facetType agains its d
		FacetType facetType = columnModel.getFacetType();
		ColumnType columnType = columnModel.getColumnType();
		
		if(facetType != null){
			switch(columnType){
			//integers and strings can be any type of facet
			case INTEGER:
			case STRING:
				break;
			//booleans and userids can only be faceted by enumeration
			case USERID:
			case BOOLEAN:
				if(facetType != FacetType.enumeration)
					throw new IllegalArgumentException("Boolean columns can only be enumeration faceted");
				break;
			//doubles and dates can only be range faceted
			case DOUBLE:
			case DATE:
				if(facetType != FacetType.range)
					throw new IllegalArgumentException("Date columns can only be ranges");
				break;
			//files, entities, links, and largetexts can not be faceted
			default:
				throw new IllegalArgumentException("The ColumnType:" + columnType + " can not be faceted");
			}
		}
	}
	

	@Override
	public ColumnModel getColumnModel(UserInfo user, String columnId) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(columnId == null) throw new IllegalArgumentException("ColumnId cannot be null");
		return columnModelDao.getColumnModel(columnId);
	}

	@Override
	public List<ColumnModel> getColumnModel(UserInfo user, List<String> ids, boolean keepOrder)
			throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(ids == null) throw new IllegalArgumentException("ColumnModel IDs cannot be null");
		return columnModelDao.getColumnModel(ids, keepOrder);
	}
	
	@WriteTransaction
	@Override
	public boolean bindColumnToObject(UserInfo user, List<String> columnIds, String objectId) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		// Get the columns and validate the size
		validateSchemaSize(columnIds);
		// pass it along to the DAO.
		long count = columnModelDao.bindColumnToObject(columnIds, objectId);
		return count > 0;
	}
	
	/**
	 * Validate that the given columns are under the maximum schema size supported.
	 * @param columnIds
	 */
	@Override
	public List<ColumnModel> validateSchemaSize(List<String> columnIds) {
		List<ColumnModel> schema = null;
		if(columnIds != null && !columnIds.isEmpty()){
			if(columnIds.size() >= MY_SQL_MAX_COLUMNS_PER_TABLE){
				throw new IllegalArgumentException("Too many columns. The limit is "+MY_SQL_MAX_COLUMNS_PER_TABLE+" columns per table");
			}
			// fetch the columns
			schema = columnModelDao.getColumnModel(columnIds, false);
			// Calculate the max row size for this schema.
			int shemaSize = TableModelUtils.calculateMaxRowSize(schema);
			if(shemaSize > MY_SQL_MAX_BYTES_PER_ROW){
				throw new IllegalArgumentException("Too much data per column. The maximum size for a row is about "+MY_SQL_MAX_BYTES_PER_ROW+" bytes. The size for the given columns would be "+shemaSize+" bytes");
			}
		}
		return schema;
	}
	
	@Override
	public List<String> calculateNewSchemaIdsAndValidate(String tableId, List<ColumnChange> changes) {
		// lookup the current schema.
		List<ColumnModel> oldSchema =  columnModelDao.getColumnModelsForObject(tableId);
		List<String> newSchemaIds = new LinkedList<>();
		for(ColumnModel cm: oldSchema){
			newSchemaIds.add(cm.getId());
		}
		// Calculate new schema
		for(ColumnChange change: changes){
			if(change.getNewColumnId() != null && change.getOldColumnId() != null){
				// update
				int oldIndex = newSchemaIds.indexOf(change.getOldColumnId());
				if(oldIndex < 0){
					throw new IllegalArgumentException("Cannot update column: "+change.getOldColumnId()+" since it is not currently a column of table: "+tableId);
				}
				newSchemaIds.add(oldIndex, change.getNewColumnId());
				newSchemaIds.remove(change.getOldColumnId());
			}else if(change.getOldColumnId() != null){
				// remove
				newSchemaIds.remove(change.getOldColumnId());
			}else{
				// add
				newSchemaIds.add(change.getNewColumnId());
			}
		}
		// Validate the new schema size.
		List<ColumnModel> newSchema = validateSchemaSize(newSchemaIds);
		// validate the schema change
		List<ColumnModel> allColumns = new LinkedList<>(oldSchema);
		allColumns.addAll(newSchema);
		validateSchemaChange(allColumns, changes);
		return newSchemaIds;
	}
	
	/**
	 * Validate each column change.
	 * @param allColumns All of the columns including the old and new schemas.
	 * @param chagnes
	 */
	static public void validateSchemaChange(List<ColumnModel> allColumns, List<ColumnChange> changes){
		// Map the IDs to columns
		Map<String, ColumnModel> idToColumnMap = new HashMap<>(allColumns.size());
		for(ColumnModel cm: allColumns){
			idToColumnMap.put(cm.getId(), cm);
		}
		// Validate each change
		for(ColumnChange change: changes){
			ColumnModel oldColumn = null;
			ColumnModel newColumn = null;
			if(change.getNewColumnId() != null){
				newColumn = idToColumnMap.get(change.getNewColumnId());
			}
			if(change.getOldColumnId() != null){
				oldColumn = idToColumnMap.get(change.getOldColumnId());
			}
			validateColumnChange(oldColumn, newColumn);
		}
	}
	
	/**
	 * Validate a single column change.
	 * @param oldColumn
	 * @param newColumn
	 */
	static void validateColumnChange(ColumnModel oldColumn, ColumnModel newColumn){
		if(oldColumn != null && newColumn != null){
			if(isFileHandleColumn(oldColumn) && !isFileHandleColumn(newColumn)){
				throw new IllegalArgumentException(String.format(COLUMN_TYPE_ERROR_TEMPLATE, ColumnType.FILEHANDLEID, newColumn.getColumnType()));
			}
			if(isFileHandleColumn(newColumn) && !isFileHandleColumn(oldColumn)){
				throw new IllegalArgumentException(String.format(COLUMN_TYPE_ERROR_TEMPLATE, oldColumn.getColumnType(), ColumnType.FILEHANDLEID));
			}
		}
	}
	
	/**
	 * Is the given ColumnModel a FileHandleId Column?
	 * 
	 * @param columnModel
	 * @return
	 */
	static boolean isFileHandleColumn(ColumnModel columnModel){
		ValidateArgument.required(columnModel, "columnModel");
		return ColumnType.FILEHANDLEID.equals(columnModel.getColumnType());
	}

	@WriteTransaction
	@Override
	public void unbindAllColumnsAndOwnerFromObject(String objectId) {
		columnModelDao.unbindAllColumnsFromObject(objectId);
		columnModelDao.deleteOwner(objectId);
	}

	@Override
	public PaginatedIds listObjectsBoundToColumn(UserInfo user,	Set<String> columnIds, boolean currentOnly, long limit, long offset) {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		validateLimitOffset(limit, offset);
		if(columnIds == null) throw new IllegalArgumentException("ColumnModel IDs cannot be null");
		List<String> results = columnModelDao.listObjectsBoundToColumn(columnIds, currentOnly, limit, offset);
		long totalCount = columnModelDao.listObjectsBoundToColumnCount(columnIds, currentOnly);
		PaginatedIds page = new PaginatedIds();
		page.setResults(results);
		page.setTotalNumberOfResults(totalCount);
		return page;
	}

	@Override
	public boolean truncateAllColumnData(UserInfo user) {
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(!user.isAdmin()) throw new UnauthorizedException("Only an Administrator can call this method");
		return this.columnModelDao.truncateAllColumnData();
	}

	@Override
	public List<ColumnModel> getColumnModelsForTable(UserInfo user,
			String tableId) throws DatastoreException, NotFoundException {
		// The user must be granted read permission on the table to get the columns.
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		return columnModelDao.getColumnModelsForObject(tableId);
	}

	@Override
	public List<ColumnModel> getCurrentColumns(UserInfo user, String tableId,
			List<SelectColumn> selectColumns) throws DatastoreException, NotFoundException {
		List<ColumnModel> columns = getColumnModelsForTable(user, tableId);
		Map<String, ColumnModel> columnIdToModelMap = TableModelUtils.createStringIDtoColumnModelMap(columns);
		List<ColumnModel> results = new LinkedList<ColumnModel>();
		for (SelectColumn selectColumn : selectColumns) {
			if (selectColumn.getId() == null) {
				throw new IllegalArgumentException("column header " + selectColumn + " is not a valid column for this table");
			}
			ColumnModel columnModel = columnIdToModelMap.get(selectColumn.getId());
			results.add(columnModel);
			if (columnModel == null) {
				throw new IllegalArgumentException("column header " + selectColumn + " is not a known column for this table");
			}
		}
		return results;
	}

	@Override
	public List<ColumnChangeDetails> getColumnChangeDetails(
			List<ColumnChange> changes) {
		// Gather all of the IDs
		List<String> columnIds = new LinkedList<>();
		for(ColumnChange change: changes){
			if(change.getNewColumnId() != null){
				columnIds.add(change.getNewColumnId());
			}
			if(change.getOldColumnId() != null){
				columnIds.add(change.getOldColumnId());
			}
		}
		boolean keepOrder = false;
		List<ColumnModel> models = columnModelDao.getColumnModel(columnIds, keepOrder);
		// map the result
		Map<String, ColumnModel> map = new HashMap<String, ColumnModel>(models.size());
		for(ColumnModel cm: models){
			map.put(cm.getId(), cm);
		}
		// Build up the results
		List<ColumnChangeDetails> details = new LinkedList<>();
		for(ColumnChange change: changes){
			ColumnModel newModel = null;
			ColumnModel oldModel = null;
			if(change.getNewColumnId() != null){
				newModel = map.get(change.getNewColumnId());
			}
			if(change.getOldColumnId() != null){
				oldModel = map.get(change.getOldColumnId());
			}
			details.add(new ColumnChangeDetails(oldModel, newModel));
		}
		return details;
	}

	@Override
	public List<String> getColumnIdForTable(String id) {
		return columnModelDao.getColumnIdsForObject(id);
	}
	
}
