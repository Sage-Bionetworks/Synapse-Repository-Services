package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.SQLUtils;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class TableViewManagerImpl implements TableViewManager {
	
	public static final String PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE = "The Project type cannot be combined with any other type.";
	public static final String ETG_COLUMN_MISSING = "The view schema must include '"+EntityField.etag.name()+"' column.";
	public static final String ETAG_MISSING_MESSAGE = "The '"+EntityField.etag.name()+"' must be included to update an Entity's annotations.";
	
	/**
	 * Max columns per view is now the same as the max per table.
	 */
	public static final int MAX_COLUMNS_PER_VIEW = ColumnConstants.MY_SQL_MAX_COLUMNS_PER_TABLE;
	
	@Autowired
	ViewScopeDao viewScopeDao;
	@Autowired
	ColumnModelManager columModelManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ColumnModelDAO columnModelDao;
	@Autowired
	NodeManager nodeManager;
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableViewManager#setViewSchemaAndScope(org.sagebionetworks.repo.model.UserInfo, java.util.List, java.util.List, java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void setViewSchemaAndScope(UserInfo userInfo, List<String> schema,
			ViewScope scope, String viewIdString) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(scope, "scope");
		validateViewSchemaSize(schema);
		Long viewId = KeyFactory.stringToKey(viewIdString);
		IdAndVersion idAndVersion = IdAndVersion.parse(viewIdString);
		Set<Long> scopeIds = null;
		if(scope.getScope() != null){
			scopeIds = new HashSet<Long>(KeyFactory.stringToKey(scope.getScope()));
		}
		Long viewTypeMaks = ViewTypeMask.getViewTypeMask(scope);
		if((viewTypeMaks & ViewTypeMask.Project.getMask()) > 0) {
			if(viewTypeMaks != ViewTypeMask.Project.getMask()) {
				throw new IllegalArgumentException(PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE);
			}
		}
		// validate the scope size
		tableManagerSupport.validateScopeSize(scopeIds, viewTypeMaks);
		
		// Define the scope of this view.
		viewScopeDao.setViewScopeAndType(viewId, scopeIds, viewTypeMaks);
		// Define the schema of this view.
		columModelManager.bindColumnsToDefaultVersionOfObject(schema, viewIdString);
		// trigger an update
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
	}

	@Override
	public Set<Long> findViewsContainingEntity(String entityId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(entityId);
		Set<Long> entityPath = tableManagerSupport.getEntityPath(idAndVersion);
		return viewScopeDao.findViewScopeIntersectionWithPath(entityPath);
	}

	@Override
	public List<ColumnModel> getViewSchema(String tableId) {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		return tableManagerSupport.getColumnModelsForTable(idAndVersion);
	}

	@WriteTransaction
	@Override
	public List<ColumnModel> applySchemaChange(UserInfo user, String viewId,
			List<ColumnChange> changes, List<String> orderedColumnIds) {
		// first determine what the new Schema will be
		List<String> newSchemaIds = columModelManager.calculateNewSchemaIdsAndValidate(viewId, changes, orderedColumnIds);
		validateViewSchemaSize(newSchemaIds);
		List<ColumnModel> newSchema = columModelManager.bindColumnsToDefaultVersionOfObject(newSchemaIds, viewId);
		IdAndVersion idAndVersion = IdAndVersion.parse(viewId);
		// trigger an update.
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
		return newSchema;
	}
	
	/**
	 * Validate that the new schema is within the allowed size for views.
	 * @param newSchema
	 */
	public static void validateViewSchemaSize(List<String> newSchema) {
		if(newSchema != null) {
			if(newSchema.size() > MAX_COLUMNS_PER_VIEW) {
				throw new IllegalArgumentException("A view cannot have "+newSchema.size()+" columns.  It must have "+MAX_COLUMNS_PER_VIEW+" columns or less.");
			}
		}
	}
	
	@Override
	public List<String> getTableSchema(String tableId){
		return columModelManager.getColumnIdForTable(IdAndVersion.parse(tableId));
	}

	/**
	 * Update an Entity using data form a view.
	 * 
	 * NOTE: Each entity is updated in a separate transaction to prevent
	 * locking the entity tables for long periods of time. This also prevents
	 * deadlock.
	 * 
	 * @return The EntityId.
	 * 
	 */
	@NewWriteTransaction
	@Override
	public void updateEntityInView(UserInfo user,
			List<ColumnModel> tableSchema, SparseRowDto row) {
		ValidateArgument.required(row, "SparseRowDto");
		ValidateArgument.required(row.getRowId(), "row.rowId");
		if(row.getValues() == null || row.getValues().isEmpty()){
			// nothing to do for this row.
			return;
		}
		String entityId = KeyFactory.keyToString(row.getRowId());
		Map<String, String> values = row.getValues();
		String etag = row.getEtag();
		if(etag == null){
			/*
			 * Prior to PLFM-4249, users provided the etag as a column on the table.  
			 * View query results will now include the etag if requested, even if the 
			 * view does not have an etag column.  However, if this etag is null, then
			 * for backwards compatibility we still need to look for an etag column
			 * in the view.
			 */
			ColumnModel etagColumn = getEtagColumn(tableSchema);
			etag = values.get(etagColumn.getId());
			if(etag == null){
				throw new IllegalArgumentException(ETAG_MISSING_MESSAGE);
			}
		}
		// Get the current annotations for this entity.
		Annotations userAnnotations = nodeManager.getUserAnnotations(user, entityId);
		userAnnotations.setEtag(etag);
		boolean updated = updateAnnotationsFromValues(userAnnotations, tableSchema, values);
		if(updated){
			// save the changes.
			nodeManager.updateUserAnnotations(user, entityId, userAnnotations);
		}
	}
	
	/**
	 * Lookup the etag column from the given schema.
	 * @param schema
	 * @return
	 */
	public static ColumnModel getEtagColumn(List<ColumnModel> schema){
		for(ColumnModel cm: schema){
			if(EntityField.etag.name().equals(cm.getName())){
				return cm;
			}
		}
		throw new IllegalArgumentException(ETG_COLUMN_MISSING);
	}
	
	/**
	 * Update the passed Annotations using the given schema and values map.
	 * 
	 * @param additional
	 * @param tableSchema
	 * @param values
	 * @return
	 */
	public static boolean updateAnnotationsFromValues(Annotations additional, List<ColumnModel> tableSchema, Map<String, String> values){
		boolean updated = false;
		// process each column of the view
		for(ColumnModel column: tableSchema){
			EntityField matchedField = EntityField.findMatch(column);
			// Ignore all entity fields.
			if(matchedField == null){
				// is this column included in the row?
				if(values.containsKey(column.getId())){
					updated = true;
					// Match the column type to an annotation type.
					AnnotationType type = SQLUtils.translateColumnTypeToAnnotationType(column.getColumnType());
					String value = values.get(column.getId());
					// Unconditionally remove a current annotation.
					additional.deleteAnnotation(column.getName());
					// Add back the annotation if the value is not null
					if(value != null){
						Object objectValue = type.parseValue(value);
						additional.replaceAnnotation(column.getName(), objectValue);
					}
				}
			}
		}
		return updated;
	}
}
