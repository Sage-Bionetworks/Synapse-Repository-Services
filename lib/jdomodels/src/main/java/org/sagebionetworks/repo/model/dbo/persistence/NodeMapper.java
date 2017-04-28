package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.springframework.jdbc.core.RowMapper;

/**
 * Mapper for reading a Node from a single query result.
 * 
 */
public class NodeMapper implements RowMapper<Node> {

	@Override
	public Node mapRow(ResultSet rs, int rowNum) throws SQLException {
		RowMapper<DBONode> nodeMapper = new DBONodeMapper();
		boolean includeAnnotations = false;
		RowMapper<DBORevision> revisionMapper = new DBORevisionMapper(includeAnnotations);
		DBONode node = nodeMapper.mapRow(rs, rowNum);
		DBORevision rev = revisionMapper.mapRow(rs, rowNum);
		Node result = NodeUtils.copyFromJDO(node, rev);
		/*
		 * Use the Zero etag for any version that is not the current version.
		 * See PLFM-1420 and PLFM-4269.
		 */
		if(!node.getCurrentRevNumber().equals(rev.getRevisionNumber())){
			result.setETag(NodeConstants.ZERO_E_TAG);
		}
		return result;
	}

}
