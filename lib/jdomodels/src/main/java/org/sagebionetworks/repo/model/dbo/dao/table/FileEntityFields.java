package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
/**
 * Enumeration that maps the columns of a FileEntity to both the database and column model. 
 *
 */
public enum FileEntityFields {
	
	id					(COL_NODE_ID, 					TABLE_NODE, 	ColumnType.INTEGER, 		null),
	name				(COL_NODE_NAME, 				TABLE_NODE, 	ColumnType.STRING,			256L),
	createdOn			(COL_NODE_CREATED_ON, 			TABLE_NODE, 	ColumnType.DATE,			null),
	createdBy			(COL_NODE_CREATED_BY, 			TABLE_NODE, 	ColumnType.INTEGER,			null),
	etag				(COL_NODE_ETAG,		 			TABLE_NODE, 	ColumnType.STRING,			36L),
	currentVersion		(COL_CURRENT_REV, 				TABLE_NODE, 	ColumnType.INTEGER, 		null),
	parentId			(COL_NODE_PARENT_ID, 			TABLE_NODE, 	ColumnType.INTEGER,			null),
	benefactorId		(COL_NODE_BENEFACTOR_ID,		TABLE_NODE, 	ColumnType.INTEGER,			null),
	projectId			(COL_NODE_PROJECT_ID,			TABLE_NODE, 	ColumnType.INTEGER,			null),
	modifiedOn			(COL_REVISION_MODIFIED_ON,		TABLE_REVISION, ColumnType.DATE,			null),
	modifiedBy			(COL_REVISION_MODIFIED_BY,		TABLE_REVISION, ColumnType.INTEGER,			null),
	dataFileHandleId	(COL_REVISION_FILE_HANDLE_ID,	TABLE_REVISION, ColumnType.FILEHANDLEID,	null);
	
	String databaseColumnName;
	String databaseTableName;
	String tableAlias;
	ColumnType type;
	Long size;
	
	/**
	 * Defines each enum value.
	 * 
	 * @param databaseColumnName
	 * @param databaseTableName
	 * @param type
	 * @param size
	 */
	private FileEntityFields(String databaseColumnName, String databaseTableName, ColumnType type, Long size){
		this.databaseColumnName = databaseColumnName;
		this.databaseTableName = databaseTableName;
		this.type = type;
		this.size = size;
		if(TABLE_NODE.equals(databaseTableName)){
			tableAlias = "N";
		}else if(TABLE_REVISION.equals(databaseTableName)){
			tableAlias = "R";
		}else{
			throw new IllegalArgumentException("Unknown databaseTableName: "+databaseTableName);
		}
	}
	
	/**
	 * Get the ColumnModel that represents this column.
	 * @return
	 */
	public ColumnModel getColumnModel(){
		ColumnModel cm = new ColumnModel();
		cm.setName(name());
		cm.setColumnType(this.type);
		cm.setMaximumSize(this.size);
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
	 * Get the name of the database table for this field.
	 * @return
	 */
	public String getDatabaseTableName() {
		return databaseTableName;
	}
	
	/**
	 * Get the alias of the database table for this field.
	 * @return
	 */
	public String getTableAlias(){
		return tableAlias;
	}
	
	/**
	 * Get the ColumnModels that define a FileEntity.
	 * @return
	 */
	public static List<ColumnModel> getAllColumnModels(){
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		for(FileEntityFields field: FileEntityFields.values()){
			list.add(field.getColumnModel());
		}
		return list;
	}

}
