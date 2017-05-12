package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DBONodeMapper implements RowMapper<DBONode> {

	@Override
	public DBONode mapRow(ResultSet rs, int rowNum) throws SQLException {
		DBONode node = new DBONode();
		node.setId(rs.getLong(COL_NODE_ID));
		node.setParentId(rs.getLong(COL_NODE_PARENT_ID));
		if(rs.wasNull()){
			node.setParentId(null);
		}
		node.setName(rs.getString(COL_NODE_NAME));
		node.setCurrentRevNumber(rs.getLong(COL_CURRENT_REV));
		if(rs.wasNull()){
			node.setCurrentRevNumber(null);
		}
		node.seteTag(rs.getString(COL_NODE_ETAG));
		node.setCreatedBy(rs.getLong(COL_NODE_CREATED_BY));
		node.setCreatedOn(rs.getLong(COL_NODE_CREATED_ON));
		node.setType(rs.getString(COL_NODE_TYPE));
		// If the value was null we must set it to null
		if(rs.wasNull()){
			node.setBenefactorId(null);
		}
		// If the value was null we must set it to null
		if(rs.wasNull()){
			node.setProjectId(null);
		}
		node.setAlias(rs.getString(COL_NODE_ALIAS));
		return node;
	}

}
