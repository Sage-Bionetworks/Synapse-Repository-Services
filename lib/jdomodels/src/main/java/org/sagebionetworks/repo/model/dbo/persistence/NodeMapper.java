package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.springframework.jdbc.core.RowMapper;

public class NodeMapper implements RowMapper<Node> {

	@Override
	public Node mapRow(ResultSet rs, int rowNum) throws SQLException {
		boolean includeHierarchyFunctions = true;
		RowMapper<DBONode> nodeMapper = new DBONodeMapper(includeHierarchyFunctions);
		boolean includeAnnotations = false;
		RowMapper<DBORevision> revisionMapper = new DBORevisionMapper(includeAnnotations);
		DBONode node = nodeMapper.mapRow(rs, rowNum);
		DBORevision rev = revisionMapper.mapRow(rs, rowNum);
		return NodeUtils.copyFromJDO(node, rev);
	}

}
