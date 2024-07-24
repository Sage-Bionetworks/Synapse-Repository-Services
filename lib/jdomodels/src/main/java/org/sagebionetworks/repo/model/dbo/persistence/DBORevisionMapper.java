package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ACTIVITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COLUMN_MODEL_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_COMMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_DEFINING_SQL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_DESCRIPTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_ITEMS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_LABEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_REF_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_SCOPE_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_SEARCH_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_USER_ANNOS_JSON;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DBORevisionMapper implements RowMapper<DBORevision> {
	
	/*
	 * Is the annotations column included in the query?
	 */
	boolean includeAnnotations;
	
	/**
	 * Create a new mapper.
	 * 
	 * @param includeAnnotations Set to true if the annotations column is selected.
	 */
	public DBORevisionMapper(boolean includeAnnotations) {
		super();
		this.includeAnnotations = includeAnnotations;
	}

	@Override
	public DBORevision mapRow(ResultSet rs, int rowNum) throws SQLException {
		DBORevision rev = new DBORevision();
		rev.setOwner(rs.getLong(COL_REVISION_OWNER_NODE));
		rev.setRevisionNumber(rs.getLong(COL_REVISION_NUMBER));						
		rev.setActivityId(rs.getLong(COL_REVISION_ACTIVITY_ID)); 
		if(rs.wasNull()) rev.setActivityId(null); // getLong returns 0 instead of null
		rev.setLabel(rs.getString(COL_REVISION_LABEL));
		rev.setComment(rs.getString(COL_REVISION_COMMENT));
		rev.setDescription(rs.getString(COL_REVISION_DESCRIPTION));
		rev.setModifiedBy(rs.getLong(COL_REVISION_MODIFIED_BY));
		rev.setModifiedOn(rs.getLong(COL_REVISION_MODIFIED_ON));
		rev.setFileHandleId(rs.getLong(COL_REVISION_FILE_HANDLE_ID));
		if(rs.wasNull()){
			rev.setFileHandleId(null);
		}

		rev.setReferenceJson(rs.getString(COL_REVISION_REF_JSON));
		rev.setColumnModelIds(rs.getBytes(COL_REVISION_COLUMN_MODEL_IDS));
		rev.setScopeIds(rs.getBytes(COL_REVISION_SCOPE_IDS));
		rev.setItems(rs.getString(COL_REVISION_ITEMS));
		rev.setIsSearchEnabled(rs.getBoolean(COL_REVISION_SEARCH_ENABLED));
		if (rs.wasNull()) {
			rev.setIsSearchEnabled(null);
		}
		rev.setDefiningSQL(rs.getString(COL_REVISION_DEFINING_SQL));
		if(includeAnnotations){
			rev.setUserAnnotationsJSON(rs.getString(COL_REVISION_USER_ANNOS_JSON));
			rev.setEntityPropertyAnnotations(rs.getBytes(COL_REVISION_ENTITY_PROPERTY_ANNOTATIONS_BLOB));
		}
		return rev;
	}

}
