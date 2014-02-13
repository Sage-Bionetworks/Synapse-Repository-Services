package org.sagebionetworks.repo.model.query.jdo;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_BENEFACTOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.NODE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.PRIMARY_FIELDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.REVISION_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

/**
 * Provides a mapping between field names Node and the various tables used to hold Node data.
 * @author jmhill
 *
 */
public enum NodeField {

	ID				(PRIMARY_FIELDS[0],		COL_NODE_ID	,				TABLE_NODE,			NODE_ALIAS),
	NAME			(PRIMARY_FIELDS[1],		COL_NODE_NAME,				TABLE_NODE,			NODE_ALIAS),
	DESCRIPTION		(PRIMARY_FIELDS[2],		COL_NODE_DESCRIPTION,		TABLE_NODE,			NODE_ALIAS),
	PARENT_ID		(PRIMARY_FIELDS[3],		COL_NODE_PARENT_ID,			TABLE_NODE,			NODE_ALIAS),
	CREATED_BY		(PRIMARY_FIELDS[4],		COL_NODE_CREATED_BY,		TABLE_NODE,			NODE_ALIAS),
	CREATED_ON		(PRIMARY_FIELDS[5],		COL_NODE_CREATED_ON,		TABLE_NODE,			NODE_ALIAS),
	MODIFIED_BY		(PRIMARY_FIELDS[6],		COL_REVISION_MODIFIED_BY,	TABLE_REVISION,		REVISION_ALIAS),
	MODIFIED_ON		(PRIMARY_FIELDS[7],		COL_REVISION_MODIFIED_ON,	TABLE_REVISION,		REVISION_ALIAS),
	NODE_TYPE		(PRIMARY_FIELDS[8],		COL_NODE_TYPE,				TABLE_NODE,			NODE_ALIAS),
	E_TAG			(PRIMARY_FIELDS[9],		COL_NODE_ETAG,				TABLE_NODE,			NODE_ALIAS),
	VERSION_NUMBER	(PRIMARY_FIELDS[10],	COL_REVISION_NUMBER,		TABLE_REVISION,		REVISION_ALIAS),
	VERSION_COMMENT	(PRIMARY_FIELDS[11],	COL_REVISION_COMMENT,		TABLE_REVISION,		REVISION_ALIAS),
	VERSION_LABEL	(PRIMARY_FIELDS[12],	COL_REVISION_LABEL,			TABLE_REVISION,		REVISION_ALIAS),
	BENEFACTOR_ID	(PRIMARY_FIELDS[13],	COL_NODE_BENEFACTOR_ID,		TABLE_NODE,			NODE_ALIAS);
	
	private String fieldName;
	private String columnName;
	private String tableName;
	private String tableAlias;
	
	private NodeField(String fieldName, String columnName, String tableName,	String tableAlias) {
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.tableName = tableName;
		this.tableAlias = tableAlias;
	}

	/**
	 * The name of this field as it appears in node.
	 * @return
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * The name of the database column that holds this fields values.
	 * @return
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * The table name that contains this field.
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * The table alias used for this field.
	 * @return
	 */
	public String getTableAlias() {
		return tableAlias;
	}
	
	/**
	 * Lookup a primary field using a name.
	 * @param name
	 * @return
	 */
	public static NodeField getFieldForName(String name){
		for(NodeField val: NodeField.values()){
			if(val.fieldName.equals(name)) return val;
		}
		throw new IllegalArgumentException("Cannot find PrimaryField for name: "+name);
	}

}
