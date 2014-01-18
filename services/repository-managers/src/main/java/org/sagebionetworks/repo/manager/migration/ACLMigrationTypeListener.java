package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;

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
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
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
	private DBOBasicDao dboBasicDao;

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
			if (acl.getOwnerType()!=null) continue;
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
	
	private static RowMapper<DBOAccessControlList> aclRowMapper = (new DBOAccessControlList()).getTableMapping();

	private static final String SELECT_FOR_UPDATE_BY_OWNER_ID_ONLY = "SELECT * FROM "+TABLE_ACCESS_CONTROL_LIST+
			" WHERE "+COL_ACL_OWNER_ID+" = :" + COL_ACL_OWNER_ID+" FOR UPDATE";

	private void updateOwnerType(final long ownerId, ObjectType ownerType) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_ACL_OWNER_ID, ownerId);
		DBOAccessControlList dbo = simpleJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_BY_OWNER_ID_ONLY, aclRowMapper, param);
		dbo.setOwnerType(ownerType);
		dboBasicDao.update(dbo);

	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// DO NOTHING
	}

}
