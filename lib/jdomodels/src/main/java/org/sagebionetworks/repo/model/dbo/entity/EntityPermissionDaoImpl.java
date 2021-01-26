package org.sagebionetworks.repo.model.dbo.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EntityPermissionDaoImpl implements EntityPermissionDao {

	public static final String GET_ENTITY_PERMISSION_SQL = DDLUtilsImpl
			.loadSQLFromClasspath("sql/EntityPermissionDao/GetEntityPermissions.sql");

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Override
	public List<EntityPermission> getEntityPermissions(Set<Long> userGroups, List<Long> entityIds) {
		ValidateArgument.required(userGroups, "userGroups");
		if (userGroups.isEmpty()) {
			throw new IllegalArgumentException("User's groups cannot be empty");
		}
		ValidateArgument.required(entityIds, "entityIdss");
		if (entityIds.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<Long, EntityPermission> results = new LinkedHashMap<Long, EntityPermission>(entityIds.size());
		for (Long entityId : entityIds) {
			results.put(entityId, new EntityPermission(entityId));
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("usersGroups", userGroups);
		params.addValue("entityIds", entityIds);
		namedJdbcTemplate.query(GET_ENTITY_PERMISSION_SQL, params, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {				
				EntityPermission permission = results.get(rs.getLong("ENTITY_ID"));
				permission.withBenefactorId(rs.getLong("BENEFACTOR_ID"));
				permission.withtHasRead(rs.getLong("READ_COUNT") > 0);
				permission.withHasDownload(rs.getLong("DOWNLOAD_COUNT") > 0);
			}
		});
		return new ArrayList<EntityPermission>((results.values()));
	}

}
