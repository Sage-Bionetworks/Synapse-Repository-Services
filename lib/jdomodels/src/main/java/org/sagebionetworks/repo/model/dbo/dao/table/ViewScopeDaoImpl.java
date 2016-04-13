package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_CONTAINER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SCOPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ViewScopeDaoImpl implements ViewScopeDao {
	
	private static final String SQL_SELECT_DISTINCT_VIEW_IDS_FOR_PATH = "SELECT DISTINCT "+COL_VIEW_SCOPE_VIEW_ID+" FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_CONTAINER_ID+" IN (:pathIds)";

	private static final String SQL_INSERT_VIEW_SCOPE = "INSERT INTO "+TABLE_VIEW_SCOPE+" ("+COL_VIEW_SCOPE_ID+", "+COL_VIEW_SCOPE_VIEW_ID+", "+COL_VIEW_SCOPE_CONTAINER_ID+") VALUES (?,?,?)";

	private static final String SQL_DELETE_ALL_FOR_VIEW_ID = "DELETE FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_VIEW_ID+" = ?";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@WriteTransactionReadCommitted
	@Override
	public void setViewScope(Long viewId, Set<Long> containerIds) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(containerIds, "containerId");
		// clear any existing scope for this view
		jdbcTemplate.update(SQL_DELETE_ALL_FOR_VIEW_ID, viewId);
		
		// add the containers
		if(!containerIds.isEmpty()){
			Iterator<Long> it = containerIds.iterator();
			while(it.hasNext()){
				Long containerId = it.next();
				if(containerId != null){
					long id = idGenerator.generateNewId(IdGenerator.TYPE.VIEW_SCOPE_ID);
					jdbcTemplate.update(SQL_INSERT_VIEW_SCOPE, id,viewId,containerId);
				}
			}
		}
	}

	@Override
	public Set<Long> findViewScopeIntersectionWithPath(Set<Long> pathIds) {
		Map<String, Set<Long>> params = new HashMap<String, Set<Long>>(1);
		params.put("pathIds", pathIds);
		List<Long> list = namedParameterJdbcTemplate.queryForList(SQL_SELECT_DISTINCT_VIEW_IDS_FOR_PATH, params, Long.class);
		return new HashSet<Long>(list);
	}

	@WriteTransactionReadCommitted
	public void truncateAll(){
		jdbcTemplate.update("TRUNCATE TABLE "+TABLE_VIEW_SCOPE);
	}
}
