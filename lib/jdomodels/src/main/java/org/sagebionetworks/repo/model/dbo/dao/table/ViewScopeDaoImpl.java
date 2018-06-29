package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_CONTAINER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_SCOPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VIEW_TYPE_VIEW_TYPE_MASK;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_SCOPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VIEW_TYPE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ViewScopeDaoImpl implements ViewScopeDao {
	
	private static final String SQL_TRUNCATE_TABLE = "DELETE FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" IS NOT NULL";

	private static final String SQL_SELECT_VIEW_TYPE = "SELECT "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" FROM "+TABLE_VIEW_TYPE+" WHERE "+COL_VIEW_TYPE_VIEW_ID+" = ?";

	private static final String SQL_INSERT_VIEW_TYPE = "INSERT INTO "+TABLE_VIEW_TYPE+" ("+COL_VIEW_TYPE_VIEW_ID+", "+COL_VIEW_TYPE_VIEW_TYPE_MASK+", "+COL_VIEW_TYPE_ETAG+") VALUES(?,?,?) ON DUPLICATE KEY UPDATE "+COL_VIEW_TYPE_VIEW_TYPE_MASK+" = ?, "+COL_VIEW_TYPE_ETAG+" = ?";

	private static final String SQL_SELECT_CONTAINERS_FOR_VIEW = "SELECT "+COL_VIEW_SCOPE_CONTAINER_ID+" FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_VIEW_ID+" = ?";

	private static final String SQL_SELECT_DISTINCT_VIEW_IDS_FOR_PATH = "SELECT DISTINCT "+COL_VIEW_SCOPE_VIEW_ID+" FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_CONTAINER_ID+" IN (:pathIds)";

	private static final String SQL_INSERT_VIEW_SCOPE = "INSERT INTO "+TABLE_VIEW_SCOPE+" ("+COL_VIEW_SCOPE_VIEW_ID+", "+COL_VIEW_SCOPE_CONTAINER_ID+") VALUES (?,?)";

	private static final String SQL_DELETE_ALL_FOR_VIEW_ID = "DELETE FROM "+TABLE_VIEW_SCOPE+" WHERE "+COL_VIEW_SCOPE_VIEW_ID+" = ?";
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@WriteTransactionReadCommitted
	@Override
	public void setViewScopeAndType(Long viewId, Set<Long> containerIds, Long viewTypeMask) {
		ValidateArgument.required(viewId, "viewId");
		ValidateArgument.required(viewTypeMask, "viewTypeMask");
		
		String etag = UUID.randomUUID().toString();
		jdbcTemplate.update(SQL_INSERT_VIEW_TYPE,viewId, viewTypeMask, etag, viewTypeMask, etag);
		
		// clear any existing scope for this view
		jdbcTemplate.update(SQL_DELETE_ALL_FOR_VIEW_ID, viewId);
		
		// add the containers
		if(containerIds != null){
			Iterator<Long> it = containerIds.iterator();
			while(it.hasNext()){
				Long containerId = it.next();
				if(containerId != null){
					jdbcTemplate.update(SQL_INSERT_VIEW_SCOPE, viewId,containerId);
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
		jdbcTemplate.update(SQL_TRUNCATE_TABLE);
	}

	@Override
	public Set<Long> getViewScope(Long viewId) {
		List<Long> list = jdbcTemplate.queryForList(SQL_SELECT_CONTAINERS_FOR_VIEW, Long.class, viewId);
		return new HashSet<Long>(list);
	}

	@Override
	public Long getViewTypeMask(Long tableId) {
		return jdbcTemplate.queryForObject(SQL_SELECT_VIEW_TYPE, Long.class, tableId);
	}
}
