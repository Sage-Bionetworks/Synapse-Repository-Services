package org.sagebionetworks.repo.manager.table;

import static org.sagebionetworks.repo.model.table.ColumnConstants.MY_SQL_MAX_BYTES_PER_ROW;
import static org.sagebionetworks.repo.model.table.ColumnConstants.MY_SQL_MAX_COLUMNS_PER_TABLE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entity.IdAndVersionBuilder;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
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
	@Autowired
	ColumnModelDAO columnModelDao;
	
	@Autowired
	NodeDAO nodeDao;
	
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
		ValidateArgument.required(columnModel, "columnModel");
		List<ColumnModel> results = createColumnModels(user, Lists.newArrayList(columnModel));
		return results.get(0);
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

	/**
	 * Validate the column model.
	 * 
	 * @param columnModel
	 */
	static void validateColumnModel(ColumnModel columnModel) {
		ValidateArgument.required(columnModel, "ColumnModel");
		checkColumnNaming(columnModel.getName());
		validateFacetType(columnModel);
	}
	
	/**
	 * Validate the column name.
	 * 
	 * @param columnModel
	 */
	static void checkColumnNaming(String name) {
		ValidateArgument.required(name, "name");
		if(name.length() > TableConstants.MAX_COLUMN_NAME_SIZE_CHARS) {
			throw new IllegalArgumentException("Column name must be: "+TableConstants.MAX_COLUMN_NAME_SIZE_CHARS+" characters or less.");
		}
		// Validate the name
		if (TableConstants.isReservedColumnName(name)) {
			throw new IllegalArgumentException("The column name: " + name + " is a system reserved column name.");
		}
	}
	
	static void validateFacetType(ColumnModel columnModel){
		//validate the facetType agains its d
		FacetType facetType = columnModel.getFacetType();
		ColumnType columnType = columnModel.getColumnType();
		
		if(facetType != null){
			switch(columnType){
			//integers and strings can be any type of facet
			case INTEGER:
			case STRING:
					break;
			//booleans, userIds, and entityIds can only be faceted by enumeration
			case USERID:
			case ENTITYID:
			case SUBMISSIONID:
			case EVALUATIONID:
			case BOOLEAN:
			case BOOLEAN_LIST:
			case STRING_LIST:
			case DATE_LIST:
			case INTEGER_LIST:
			case USERID_LIST:
			case ENTITYID_LIST:
					if(facetType != FacetType.enumeration)
					throw new IllegalArgumentException(columnType + " columns can only be enumeration faceted");
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

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.ColumnModelManager#getColumnModels(java.util.List)
	 */
	@Override
	public List<ColumnModel> getAndValidateColumnModels(List<String> ids)
			throws DatastoreException, NotFoundException {
		ValidateArgument.required(ids, "ColumnModel IDs");
		List<ColumnModel> fromDb =  columnModelDao.getColumnModels(ids);
		Map<String, ColumnModel> resultMap = TableModelUtils.createIdToColumnModelMap(fromDb);
		// column IDs must be unique.
		Set<String> visitedIds = new HashSet<>(fromDb.size());
		// column names must be unique.
		Set<String> visitedNames = new HashSet<>(fromDb.size());
		List<ColumnModel> results = new LinkedList<>();
		for(String id: ids) {
			ColumnModel cm = resultMap.get(id);
			if (cm == null) {
				throw new NotFoundException("Column does not exist for id: " + id);
			}
			if (!visitedIds.add(id)) {
				throw new IllegalArgumentException("Duplicate column: '" + cm.getName() + "'");
			}
			if (!visitedNames.add(cm.getName())) {
				throw new IllegalArgumentException("Duplicate column name: '" + cm.getName() + "'");
			}
			results.add(cm);
		}
		return results;
	}
	
	@WriteTransaction
	@Override
	public List<ColumnModel> bindColumnsToDefaultVersionOfObject(List<String> columnIds, String objectId) throws DatastoreException, NotFoundException {
		// Use null version to set the default schema of the table.
		IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(KeyFactory.stringToKey(objectId)).setVersion(null)
				.build();
		return bindColumnsToVersionOfObject(columnIds, idAndVersion);
	}
	

	@Override
	public List<ColumnModel> bindCurrentColumnsToVersion(IdAndVersion idAndVersion) {
		// Lookup the current columns for the given object
		List<String> currentSchema = columnModelDao.getColumnModelIdsForObject(IdAndVersion.newBuilder().setId(idAndVersion.getId()).build());
		// bind the current schema to the given id and version pair.
		return bindColumnsToVersionOfObject(currentSchema, idAndVersion);
	}
	
	@Override
	public List<ColumnModel> bindColumnsToVersionOfObject(List<String> columnIds, IdAndVersion idAndVersion)
			throws DatastoreException, NotFoundException {
		// Get the columns and validate the size
		List<ColumnModel> schema = validateSchemaSize(columnIds);
		columnModelDao.bindColumnToObject(schema, idAndVersion);
		return schema;
	}
	
	/**
	 * Validate that the given columns are under the maximum schema size supported.
	 * @param columnIds
	 */
	@Override
	public List<ColumnModel> validateSchemaSize(List<String> columnIds) {
		if (columnIds == null || columnIds.isEmpty()) {
			return new LinkedList<ColumnModel>();
		}
		if (columnIds.size() > MY_SQL_MAX_COLUMNS_PER_TABLE) {
			throw new IllegalArgumentException(
					"Too many columns. The limit is "
							+ MY_SQL_MAX_COLUMNS_PER_TABLE
							+ " columns per table");
		}
		// fetch the columns
		List<ColumnModel> schema = getAndValidateColumnModels(columnIds);
		// Calculate the max row size for this schema.
		int shemaSize = TableModelUtils.calculateMaxRowSize(schema);
		if (shemaSize > MY_SQL_MAX_BYTES_PER_ROW) {
			throw new IllegalArgumentException("Too much data per column. The maximum size for a row is about "
							+ MY_SQL_MAX_BYTES_PER_ROW
							+ " bytes. The size for the given columns would be "
							+ shemaSize + " bytes");
		}
		return schema;
	}
	
	@Override
	public List<String> calculateNewSchemaIdsAndValidate(String tableId, List<ColumnChange> changes,
			List<String> orderedColumnIds) {
		// lookup the current schema.
		List<ColumnModel> oldSchema =  columnModelDao.getColumnModelsForObject(IdAndVersion.parse(tableId));
		List<String> newSchemaIds = new LinkedList<>();
		// start with all of the old Ids.
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
		if (orderedColumnIds != null) {
			validateSchemaWithProvidedOrderedColumns(newSchemaIds, orderedColumnIds);
			newSchemaIds = orderedColumnIds;
		}
		// Validate the new schema size.
		List<ColumnModel> newSchema = validateSchemaSize(newSchemaIds);
		// validate the schema change
		List<ColumnModel> allColumns = new LinkedList<>(oldSchema);
		allColumns.addAll(newSchema);
		EntityType entityType = nodeDao.getNodeTypeById(tableId);
		validateSchemaChange(allColumns, changes, entityType);
		return newSchemaIds;
	}
	
	public static void validateSchemaWithProvidedOrderedColumns(List<String> newSchemaIds, List<String> orderedColumnIds) {
		ValidateArgument.required(newSchemaIds, "newSchemaIds");
		ValidateArgument.required(orderedColumnIds, "orderedColumnIds");
		Set<String> newSchemaSet = new HashSet<>(newSchemaIds);
		Set<String> ordered = new HashSet<>(orderedColumnIds);
		ValidateArgument.requirement(ordered.equals(newSchemaSet),
				"The provided ordered column IDs does not match resulting columns in the table schema.");
	}

	/**
	 * Validate each column change.
	 * @param allColumns All of the columns including the old and new schemas.
	 * @param chagnes
	 */
	static public void validateSchemaChange(List<ColumnModel> allColumns, List<ColumnChange> changes, EntityType entityType){
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
			validateColumnChange(oldColumn, newColumn, entityType);
		}
	}
	
	/**
	 * Validate a single column change.
	 * @param oldColumn
	 * @param newColumn
	 */
	static void validateColumnChange(ColumnModel oldColumn, ColumnModel newColumn, EntityType entityType){
		if(oldColumn != null && newColumn != null){
			if(isFileHandleColumn(oldColumn) && !isFileHandleColumn(newColumn)){
				throw new IllegalArgumentException(String.format(COLUMN_TYPE_ERROR_TEMPLATE, ColumnType.FILEHANDLEID, newColumn.getColumnType()));
			}
			if(isFileHandleColumn(newColumn) && !isFileHandleColumn(oldColumn)){
				throw new IllegalArgumentException(String.format(COLUMN_TYPE_ERROR_TEMPLATE, oldColumn.getColumnType(), ColumnType.FILEHANDLEID));
			}

			//table can not support changing schema to or from list types
			if(entityType == EntityType.table){
				if( (ColumnTypeListMappings.isList(oldColumn.getColumnType()) || ColumnTypeListMappings.isList(newColumn.getColumnType()))
					&& !oldColumn.getColumnType().equals(newColumn.getColumnType())//keeping the same type for operations such as rename are allowed
				){
					throw new IllegalArgumentException("Can not perform schema change on _LIST type columns for Table Entities");
				}
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
		columnModelDao.deleteOwner(objectId);
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
		authorizationManager.canAccess(user, tableId, ObjectType.ENTITY, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return columnModelDao.getColumnModelsForObject(IdAndVersion.parse(tableId));
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
		List<ColumnModel> models = columnModelDao.getColumnModels(columnIds);
		Map<String, ColumnModel> map = TableModelUtils.createIdToColumnModelMap(models);
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

	/**
	 * The schema for the current version for any table/view only exists in the
	 * database with a null version number. Therefore, calls to get the schema of
	 * the current version are transformed to a representation with a null
	 * version.
	 * 
	 * @param inputIdAndVersion
	 * @return
	 */
	IdAndVersion removeVersionAsNeeded(IdAndVersion inputIdAndVersion) {
		IdAndVersionBuilder resultBuilder = IdAndVersion.newBuilder();
		resultBuilder.setId(inputIdAndVersion.getId());
		if (inputIdAndVersion.getVersion().isPresent()) {
			/*
			 * The current version of any table is always 'in progress' and does not have a
			 * schema bound to it. This means the schema for the current version always
			 * matches the latest schema for the table. Therefore, when a caller explicitly
			 * requests the schema of the current version, the latest schema is returned.
			 */
			long currentVersion = nodeDao.getCurrentRevisionNumber(inputIdAndVersion.getId().toString());
			long inputVersion = inputIdAndVersion.getVersion().get();
			if (inputVersion != currentVersion) {
				// Only use the input version number when it is not the current version.
				resultBuilder.setVersion(inputVersion);
			}
		}
		return resultBuilder.build();
	}
	
	@Override
	public List<String> getColumnIdsForTable(IdAndVersion idAndVersion) {
		return columnModelDao.getColumnModelIdsForObject(removeVersionAsNeeded(idAndVersion));
	}

	@Override
	public List<ColumnModel> getColumnModelsForObject(IdAndVersion idAndVersion) {
		return columnModelDao.getColumnModelsForObject(removeVersionAsNeeded(idAndVersion));
	}

	@Override
	public ColumnModel createColumnModel(ColumnModel columnModel) {
		return columnModelDao.createColumnModel(columnModel);
	}
	
}
