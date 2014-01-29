package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList.OWNER_ID_FIELD_NAME;
import static org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList.OWNER_TYPE_FIELD_NAME;

import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a temporary listener to make sure that each ACL has an OWNER_TYPE
 * 
 * @author brucehoff
 *
 */
public class ACLMigrationTypeListener implements MigrationTypeListener {
	
	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;	

	@Autowired
	TeamDAO teamDAO;
	
	@Autowired
	NodeDAO nodeDAO;
	
	@Autowired
	EvaluationDAO evaluationDAO;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		if (!type.equals(MigrationType.ACL)) return;
		for (D d : delta) {
			DBOAccessControlList acl = (DBOAccessControlList)d;
			idGenerator.reserveId(acl.getId(), TYPE.ACL_ID);
			if (!acl.getOwnerType().equals(DBOAccessControlList.UNKNOWN_OWNER_TYPE)) continue;
			// we need to set the owner type.  It's either ENTITY, EVALUATION, or TEAM
			Long ownerId = acl.getOwnerId();
			String ownerIdString = ownerId.toString();
			try {
				Node node = nodeDAO.getNode(ownerIdString);
				if (node!=null) {
					updateOwnerType(ownerId, ObjectType.ENTITY);
					continue;
				}
			} catch (NotFoundException e) {
				// try another type
			}
			try {
				Team team = teamDAO.get(ownerIdString);
				if (team!=null) {
					updateOwnerType(ownerId, ObjectType.TEAM);
					continue;
				}
			} catch (NotFoundException e) {
				// try another type
			}
			try {
				Evaluation evaluation = evaluationDAO.get(ownerIdString);
				if (evaluation!=null) {
					updateOwnerType(ownerId, ObjectType.EVALUATION);
					continue;
				}
			} catch (NotFoundException e) {
				// try another type
			}
			throw new RuntimeException("ID: "+ownerId+" is not the ID of an Entity, Team or Evaluation");
		}

	}
	
	private static final String OWNER_TYPE_UPDATE_SQL = "update ACL set OWNER_TYPE=:"+OWNER_TYPE_FIELD_NAME+" where OWNER_ID=:"+OWNER_ID_FIELD_NAME;

	private void updateOwnerType(final long ownerId, ObjectType ownerType) {
		MapSqlParameterSource updateParam = new MapSqlParameterSource();
		updateParam.addValue(OWNER_ID_FIELD_NAME,ownerId);
		updateParam.addValue(OWNER_TYPE_FIELD_NAME, ownerType.name());
		simpleJdbcTemplate.update(OWNER_TYPE_UPDATE_SQL, updateParam);
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// DO NOTHING
	}

}
