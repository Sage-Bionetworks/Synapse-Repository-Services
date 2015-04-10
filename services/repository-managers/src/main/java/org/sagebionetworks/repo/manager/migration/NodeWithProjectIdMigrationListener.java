package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.util.List;

import javax.annotation.PostConstruct;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class NodeWithProjectIdMigrationListener implements MigrationTypeListener {

	private static final String UPDATE_PROJECT_SQL = "UPDATE " + TABLE_NODE + " SET " + COL_NODE_PROJECT_ID + " = ? WHERE " + COL_NODE_ID
			+ " = ?";

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private EntityType PROJECT_ENTITY_TYPE;

	@PostConstruct
	private void getProjectEntityType() {
		PROJECT_ENTITY_TYPE = EntityType.getNodeTypeForClass(Project.class);
	}

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type != MigrationType.NODE) {
			return;
		}

		for (D d : delta) {
			DBONode node = (DBONode) d;

			if (node.getParentId() == null) {
				// skip root and parentless nodes
				continue;
			}

			if (node.getProjectId() != null) {
				// skip nodes that already have their project id set
				continue;
			}

			if (node.getNodeType().equals(PROJECT_ENTITY_TYPE.getId())) {
				// project nodes have themselves as the project
				jdbcTemplate.update(UPDATE_PROJECT_SQL, node.getId(), node.getId());
			} else {
				// get the project id of the parent node
				MapSqlParameterSource params = new MapSqlParameterSource("id", node.getParentId());
				DBONode parentNode = dboBasicDao.getObjectByPrimaryKey(DBONode.class, params);
				jdbcTemplate.update(UPDATE_PROJECT_SQL, parentNode.getProjectId(), node.getId());
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// nothing here
	}
}
