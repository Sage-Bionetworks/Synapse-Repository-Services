package org.sagebionetworks.repo.model.dbo.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsersEntityPermissionsDaoImpl implements UsersEntityPermissionsDao {

	public static final String GET_ENTITY_PERMISSION_SQL = DDLUtilsImpl
			.loadSQLFromClasspath("sql/GetEntityPermissions.sql");

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Override
	public Map<Long, UserEntityPermissionsState> getEntityPermissionsAsMap(Set<Long> userGroups, List<Long> entityIds) {
		ValidateArgument.required(userGroups, "userGroups");
		if (userGroups.isEmpty()) {
			throw new IllegalArgumentException("User's groups cannot be empty");
		}
		ValidateArgument.required(entityIds, "entityIds");
		if (entityIds.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashMap<Long, UserEntityPermissionsState> results = new LinkedHashMap<Long, UserEntityPermissionsState>(entityIds.size());
		for (Long entityId : entityIds) {
			results.put(entityId, new UserEntityPermissionsState(entityId));
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("usersGroups", userGroups);
		params.addValue("entityIds", entityIds);
		params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
		params.addValue("publicId", BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		namedJdbcTemplate.query(GET_ENTITY_PERMISSION_SQL, params, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				UserEntityPermissionsState permission = results.get(rs.getLong("ENTITY_ID"));
				permission.withtDoesEntityExist(true);
				permission.withBenefactorId(rs.getLong("BENEFACTOR_ID"));
				permission.withEntityType(EntityType.valueOf(rs.getString("ENTITY_TYPE")));
				permission.withEntityParentId(rs.getLong("ENTITY_PARENT_ID"));
				if(rs.wasNull()) {
					permission.withEntityParentId(null);
				}
				permission.withEntityCreatedBy(rs.getLong("ENTITY_CREATED_BY"));
				String dataType = rs.getString("DATA_TYPE");
				if (dataType != null) {
					permission.withDataType(DataType.valueOf(dataType));
				}
				permission.withHasChangePermissions(rs.getLong("CHANGE_PERMISSIONS_COUNT") > 0);
				permission.withHasChangeSettings(rs.getLong("CHANGE_SETTINGS_COUNT") > 0);
				permission.withHasCreate(rs.getLong("CREATE_COUNT") > 0);
				permission.withHasUpdate(rs.getLong("UPDATE_COUNT") > 0);
				permission.withHasDelete(rs.getLong("DELETE_COUNT") > 0);
				permission.withHasDownload(rs.getLong("DOWNLOAD_COUNT") > 0);
				permission.withHasRead(rs.getLong("READ_COUNT") > 0);
				permission.withHasModerate(rs.getLong("MODERATE_COUNT") > 0);
				permission.withHasPublicRead(rs.getLong("PUBLIC_READ_COUNT") > 0);
			}
		});
		return results;
	}

	@Override
	public List<UserEntityPermissionsState> getEntityPermissions(Set<Long> usersPrincipalIds, List<Long> entityIds) {
		return new ArrayList<UserEntityPermissionsState>(getEntityPermissionsAsMap(usersPrincipalIds, entityIds).values());
	}

}
