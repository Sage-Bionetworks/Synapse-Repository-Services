package org.sagebionetworks.repo.model.table;

import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_CRATED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_CRATED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_FILE_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_NAME;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_PARENT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_PROJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_COL_VERSION;

import java.util.LinkedList;
import java.util.List;
/**
 * Enumeration that maps the columns of a ENTITY_REPLICATION to both the database and column model. 
 *
 */
public enum EntityField {
	
	id					(ENTITY_REPLICATION_COL_ID, 			ColumnType.ENTITYID, 		null,	null),
	name				(ENTITY_REPLICATION_COL_NAME,			ColumnType.STRING,			256L,	null),
	createdOn			(ENTITY_REPLICATION_COL_CRATED_ON, 		ColumnType.DATE,			null,	FacetType.range),
	createdBy			(ENTITY_REPLICATION_COL_CRATED_BY, 		ColumnType.USERID,			null,	FacetType.enumeration),
	etag				(ENTITY_REPLICATION_COL_ETAG,	 		ColumnType.STRING,			36L,	null),
	type				(ENTITY_REPLICATION_COL_TYPE,	 		ColumnType.STRING,			20L,	FacetType.enumeration),
	currentVersion		(ENTITY_REPLICATION_COL_VERSION, 		ColumnType.INTEGER, 		null,	null),
	parentId			(ENTITY_REPLICATION_COL_PARENT_ID, 		ColumnType.ENTITYID,		null,	FacetType.enumeration),
	benefactorId		(ENTITY_REPLICATION_COL_BENEFACTOR_ID, 	ColumnType.ENTITYID,		null,	null),
	projectId			(ENTITY_REPLICATION_COL_PROJECT_ID, 	ColumnType.ENTITYID,		null,	FacetType.enumeration),
	modifiedOn			(ENTITY_REPLICATION_COL_MODIFIED_ON,	ColumnType.DATE,			null,	FacetType.range),
	modifiedBy			(ENTITY_REPLICATION_COL_MODIFIED_BY,	ColumnType.USERID,			null,	FacetType.enumeration),
	dataFileHandleId	(ENTITY_REPLICATION_COL_FILE_ID,		ColumnType.FILEHANDLEID,	null,	null);
	
	String databaseColumnName;
	ColumnType colunmType;
	Long size;
	FacetType facetType;
	
	/**
	 * Defines each enum value.
	 * 
	 * @param databaseColumnName
	 * @param databaseTableName
	 * @param type
	 * @param size
	 * @param facetType
	 */
	private EntityField(String columnName, ColumnType type, Long size, FacetType facetType){
		this.databaseColumnName = columnName;
		this.colunmType = type;
		this.size = size;
		this.facetType = facetType;
	}
	
	/**
	 * Get the ColumnModel that represents this column.
	 * @return
	 */
	public ColumnModel getColumnModel(){
		ColumnModel cm = new ColumnModel();
		cm.setName(name());
		cm.setColumnType(this.colunmType);
		cm.setMaximumSize(this.size);
		cm.setFacetType(this.facetType);
		return cm;
	}

	/**
	 * Get the name of the database column for this field.
	 * @return
	 */
	public String getDatabaseColumnName() {
		return databaseColumnName;
	}
	
	/**
	 * Get the ColumnModels that define a FileEntity.
	 * @return
	 */
	public static List<ColumnModel> getAllColumnModels(){
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for(EntityField field: EntityField.values()){
			list.add(field.getColumnModel());
		}
		return list;
	}

	/**
	 * Does the given ColumnModel match this EntityField?
	 * @param cm
	 * @return
	 */
	public boolean isMatch(ColumnModel cm){
		ColumnModel fieldColumnModel = this.getColumnModel();
		// name must match
		if(!fieldColumnModel.getName().equals(cm.getName())){
			return false;
		}
		// type must match
		if(!fieldColumnModel.getColumnType().equals(cm.getColumnType())){
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
	public static EntityField findMatch(ColumnModel cm){
		for(EntityField field: EntityField.values()){
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
	public static ColumnModel findMatch(List<ColumnModel> columns, EntityField toMatch){
		for(ColumnModel cm: columns){
			if(toMatch.isMatch(cm)){
				return cm;
			}
		}
		return null;
	}

}
