package org.sagebionetworks.repo.model.table;

import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_CREATED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBEJCT_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_MD5;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_FILE_SIZE_BYTES;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PARENT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_PROJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_SUBTYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.OBJECT_REPLICATION_COL_VERSION;

import java.util.LinkedList;
import java.util.List;

/**
 * Enumeration that maps the columns of a OBJECT_REPLICATION to both the database and column model. 
 */
public enum ObjectField {
	
	id					(OBJECT_REPLICATION_COL_OBJECT_ID, 		null, 						null,	null),
	name				(OBJECT_REPLICATION_COL_NAME,			ColumnType.STRING,			256L,	null),
	createdOn			(OBJECT_REPLICATION_COL_CREATED_ON, 	ColumnType.DATE,			null,	FacetType.range),
	createdBy			(OBJECT_REPLICATION_COL_CREATED_BY, 	ColumnType.USERID,			null,	FacetType.enumeration),
	etag				(OBEJCT_REPLICATION_COL_ETAG,	 		ColumnType.STRING,			36L,	null),
	type				(OBJECT_REPLICATION_COL_SUBTYPE,	 	ColumnType.STRING,			20L,	FacetType.enumeration),
	currentVersion		(OBJECT_REPLICATION_COL_VERSION, 		ColumnType.INTEGER, 		null,	null),
	parentId			(OBJECT_REPLICATION_COL_PARENT_ID, 		null,						null,	FacetType.enumeration),
	benefactorId		(OBJECT_REPLICATION_COL_BENEFACTOR_ID, 	null,						null,	null),
	projectId			(OBJECT_REPLICATION_COL_PROJECT_ID, 	ColumnType.ENTITYID,		null,	FacetType.enumeration),
	modifiedOn			(OBJECT_REPLICATION_COL_MODIFIED_ON,	ColumnType.DATE,			null,	FacetType.range),
	modifiedBy			(OBJECT_REPLICATION_COL_MODIFIED_BY,	ColumnType.USERID,			null,	FacetType.enumeration),
	dataFileHandleId	(OBJECT_REPLICATION_COL_FILE_ID,		ColumnType.FILEHANDLEID,	null,	null),
	dataFileSizeBytes	(OBJECT_REPLICATION_COL_FILE_SIZE_BYTES,ColumnType.INTEGER,			null,	null),
	dataFileMD5Hex		(OBJECT_REPLICATION_COL_FILE_MD5,		ColumnType.STRING,			100L,	null);
	
	private String databaseColumnName;
	// Note that this column type for id, parentId and benefactorId is not known a priori and is dynamically computed
	// according to the OBJECT_TYPE of the view
	private ColumnType columnType;
	private Long size;
	private FacetType facetType;
	
	/**
	 * Defines each enum value.
	 * 
	 * @param databaseColumnName
	 * @param databaseTableName
	 * @param type
	 * @param size
	 * @param facetType
	 */
	private ObjectField(String columnName, ColumnType type, Long size, FacetType facetType){
		this.databaseColumnName = columnName;
		this.columnType = type;
		this.size = size;
		this.facetType = facetType;
	}
	
	/**
	 * @return The name of the database column for this field.
	 */
	public String getDatabaseColumnName() {
		return databaseColumnName;
	}
	
	/**
	 * @return The (max) size of the column, can be null for no limit
	 */
	public Long getSize() {
		return size;
	}
	
	/**
	 * @return The mapped type of the column, can be null when it cannot determined before-hand
	 */
	public ColumnType getColumnType() {
		return columnType;
	}
	
	/**
	 * @return The type of facet supported by the column, can be null
	 */
	public FacetType getFacetType() {
		return facetType;
	}
	
	/**
	 * Get the ColumnModel that represents this column.
	 * @return
	 */
	@Deprecated
	public ColumnModel getColumnModel(){
		ColumnModel cm = new ColumnModel();
		cm.setName(name());
		// For backwards compatibility until this is removed
		cm.setColumnType(this.columnType == null ? ColumnType.ENTITYID : this.columnType);
		cm.setMaximumSize(this.size);
		cm.setFacetType(this.facetType);
		return cm;
	}
	
	@Deprecated
	public static List<ColumnModel> getAllColumnModels(){
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for(ObjectField field: ObjectField.values()){
			list.add(field.getColumnModel());
		}
		return list;
	}

	/**
	 * Does the given ColumnModel match this EntityField?
	 * @param cm
	 * @return
	 */
	@Deprecated
	public boolean isMatch(ColumnModel cm){
		ColumnModel fieldColumnModel = this.getColumnModel();
		// name must match
		if(!fieldColumnModel.getName().equals(cm.getName())){
			return false;
		}
		// For backwards compatibility until this is removed
		ColumnType columnType = fieldColumnModel.getColumnType() == null ? ColumnType.ENTITYID : fieldColumnModel.getColumnType();
		// type must match
		if(!columnType.equals(cm.getColumnType())){
			return false;
		}
		// size must be greater than or equal
		if(fieldColumnModel.getMaximumSize() != null){
			if(cm.getMaximumSize() == null){
				return false;
			}
			if(cm.getMaximumSize() < fieldColumnModel.getMaximumSize()){
				return false;
			}
		}
		// name and type match, and size is than greater or equal
		return true;
	}
	
	/**
	 * For a given ColumnModel find the matching EntityField.
	 * 
	 * @param cm
	 * @return Returns null if there is no match.
	 */
	@Deprecated
	public static ObjectField findMatch(ColumnModel cm){
		for(ObjectField field: ObjectField.values()){
			if(field.isMatch(cm)){
				return field;
			}
		}
		return null;
	}
	
	/**
	 * Given a list of ColumnModels find the ColumnModel that matches the given EntityField.
	 * 
	 * @param columns
	 * @param toMatch
	 * @return Returns null if no match is found.
	 */
	@Deprecated
	public static ColumnModel findMatch(List<ColumnModel> columns, ObjectField toMatch){
		for(ColumnModel cm: columns){
			if(toMatch.isMatch(cm)){
				return cm;
			}
		}
		return null;
	}

}
