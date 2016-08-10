package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.PermissionDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;


public class PermissionDaoImpl implements PermissionDao {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	SELECT count(*) FROM
	JDOSUBMISSION s,
	ACL acl,
	JDORESOURCEACCESS ra,
	JDORESOURCEACCESS_ACCESSTYPE at
	WHERE
	s.entity_id=? and
	s.evaluation_id=acl.owner_id and
	acl.owner_type='EVALUATION' and
	acl.id=ra.owner_id and
	ra.group_id in (?) and
	ra.id=at.id_oid and at.string_ele=?
	*/
	
	private static final String SUBMISSIONS_WITH_ENTITY_AND_PERMISSION_SQL = 
			"SELECT COUNT(*) FROM "+
			TABLE_SUBMISSION+" s, "+
			TABLE_ACCESS_CONTROL_LIST+" acl, "+
			TABLE_RESOURCE_ACCESS+" ra, "+
			TABLE_RESOURCE_ACCESS_TYPE+" at "+
			" WHERE s."+
			COL_SUBMISSION_ENTITY_ID+"=:"+COL_SUBMISSION_ENTITY_ID+" and s."+
			COL_SUBMISSION_EVAL_ID+"=acl."+COL_ACL_OWNER_ID+" and acl."+
			COL_ACL_OWNER_TYPE+"='"+ObjectType.EVALUATION+"' and acl."+
			COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER+" and ra."+
			COL_RESOURCE_ACCESS_GROUP_ID+" in (:"+COL_RESOURCE_ACCESS_GROUP_ID+") and ra."+
			COL_RESOURCE_ACCESS_ID+"=at."+COL_RESOURCE_ACCESS_TYPE_ID+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"=:"+COL_RESOURCE_ACCESS_TYPE_ELEMENT;

	@Override
	public boolean isEntityInEvaluationWithAccess(String entityId,
			List<Long> principalIds, ACCESS_TYPE accessType) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(principalIds, "principalIds");
		ValidateArgument.required(accessType, "accessType");
		if (principalIds.isEmpty()) return false;
		
		
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_ENTITY_ID, KeyFactory.stringToKey(entityId));
		param.addValue(COL_RESOURCE_ACCESS_GROUP_ID, principalIds);
		param.addValue(COL_RESOURCE_ACCESS_TYPE_ELEMENT, accessType.name());

		return 0<namedTemplate.queryForObject(
				SUBMISSIONS_WITH_ENTITY_AND_PERMISSION_SQL, param, Long.class);
	}

}
