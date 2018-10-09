package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_JOIN;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_TABLES;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.PRINCIPAL_IDS_BIND_VAR;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.RESOURCE_ID_BIND_VAR;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Sets;

public class DBOAuthorizationDaoImpl implements AuthorizationDAO {

	private static final String BIND_PARENT_ID = "bParentId";
	private static final String BIND_GROUP_IDS = "bGroupIds";
	
	private static final String SELECT_RESOURCE_INTERSECTION = "SELECT acl."
			+ COL_ACL_OWNER_ID
			+ " as "+COL_ACL_OWNER_ID+" FROM "
			+ AUTHORIZATION_SQL_TABLES
			+ " WHERE "
			+ AUTHORIZATION_SQL_JOIN
			+ " AND ra."
			+ COL_RESOURCE_ACCESS_GROUP_ID
			+ " IN (:"
			+ PRINCIPAL_IDS_BIND_VAR
			+ ") AND at."
			+ COL_RESOURCE_ACCESS_TYPE_ELEMENT
			+ "=:"
			+ ACCESS_TYPE_BIND_VAR
			+ " AND acl."
			+ COL_ACL_OWNER_ID
			+ " IN (:"
			+ RESOURCE_ID_BIND_VAR
			+ ") AND acl." + COL_ACL_OWNER_TYPE + "=:" + RESOURCE_TYPE_BIND_VAR;
	
	private static final String SELECT_NON_VISIBLE_CHILDREN =
			"SELECT N1."+COL_NODE_ID+
				" FROM "+TABLE_NODE+" N1"+
					" JOIN "+TABLE_ACCESS_CONTROL_LIST+" A"+
						" ON (N1."+COL_NODE_ID+" = A."+COL_ACL_OWNER_ID+
							" AND N1."+COL_NODE_PARENT_ID+" = :"+BIND_PARENT_ID+
							" AND A."+COL_ACL_OWNER_TYPE+" = '"+ObjectType.ENTITY.name()+"')"+
				" WHERE N1."+COL_NODE_ID+" NOT IN ("+
							"SELECT DISTINCT N2."+COL_NODE_ID+
								" FROM "+TABLE_NODE+" N2"+
									" JOIN "+TABLE_ACCESS_CONTROL_LIST+" A2"+
										" ON (N2."+COL_NODE_ID+" = A2."+COL_ACL_OWNER_ID+
											" AND N2."+COL_NODE_PARENT_ID+" = :"+BIND_PARENT_ID+
											" AND A2."+COL_ACL_OWNER_TYPE+" = '"+ObjectType.ENTITY.name()+"')"+
									" JOIN "+TABLE_RESOURCE_ACCESS+" RA"+
										" ON (A2."+COL_ACL_ID+" = RA."+COL_RESOURCE_ACCESS_OWNER+
											" AND RA."+COL_RESOURCE_ACCESS_GROUP_ID+" IN (:"+BIND_GROUP_IDS+"))"+
									" JOIN "+TABLE_RESOURCE_ACCESS_TYPE+" AC"+
										" ON (RA."+COL_RESOURCE_ACCESS_ID+" = AC."+COL_RESOURCE_ACCESS_TYPE_ID+
											" AND AC."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+" = '"+ACCESS_TYPE.READ.name()+"'))";

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


	@Override
	public boolean canAccess(Set<Long> groups, String resourceId,
			ObjectType resourceType, ACCESS_TYPE accessType)
			throws DatastoreException {
		Long idLong = KeyFactory.stringToKey(resourceId);
		HashSet<Long> benefactors = Sets.newHashSet(idLong);
		Set<Long> results = getAccessibleBenefactors(groups, benefactors,
				resourceType, accessType);
		return results.contains(idLong);
	}

	@Override
	public Set<Long> getAccessibleBenefactors(Set<Long> groups,
			Set<Long> benefactors, ObjectType resourceType,
			ACCESS_TYPE accessType) {
		ValidateArgument.required(groups, "groups");
		ValidateArgument.required(benefactors, "benefactors");
		ValidateArgument.required(resourceType, "resourceType");
		ValidateArgument.required(accessType, "accessType");
		if (groups.isEmpty() || benefactors.isEmpty()) {
			// there will be no matches for empty inputs.
			return new HashSet<Long>(0);
		}
		Map<String, Object> namedParameters = new HashMap<String, Object>(4);
		namedParameters.put(RESOURCE_ID_BIND_VAR,
				benefactors);
		namedParameters
				.put(PRINCIPAL_IDS_BIND_VAR, groups);
		namedParameters.put(RESOURCE_TYPE_BIND_VAR,
				resourceType.name());
		namedParameters.put(ACCESS_TYPE_BIND_VAR,
				accessType.name());
		// query
		List<Long> result = namedParameterJdbcTemplate.query(
				SELECT_RESOURCE_INTERSECTION,
				namedParameters, new RowMapper<Long>() {

					@Override
					public Long mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getLong(COL_ACL_OWNER_ID);
					}
				});
		return new HashSet<Long>(result);
	}

	@Override
	public Set<Long> getNonVisibleChildrenOfEntity(Set<Long> groups,
			String parentId) {
		ValidateArgument.required(groups, "groups");
		ValidateArgument.requirement(!groups.isEmpty(), "Must have at least one group ID");
		ValidateArgument.required(parentId, "parentId");
		Map<String, Object> namedParameters = new HashMap<String, Object>(1);
		namedParameters.put(BIND_PARENT_ID, KeyFactory.stringToKey(parentId));
		namedParameters.put(BIND_GROUP_IDS, groups);
		final Set<Long> results = new HashSet<>();
		namedParameterJdbcTemplate.query(SELECT_NON_VISIBLE_CHILDREN, namedParameters, new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				results.add(rs.getLong(COL_NODE_ID));
			}
		});
		return results;
	}

}
