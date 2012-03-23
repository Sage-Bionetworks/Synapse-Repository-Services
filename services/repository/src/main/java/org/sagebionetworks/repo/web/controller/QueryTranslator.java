package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.web.query.QueryStatement;

/**
 * As simple utility to translate from a web-service query to an internal query.
 * @author jmhill
 *
 */
public class QueryTranslator {
	
	public static String ENTITY = "entity";
	
	/**
	 * Create a BasicQuery from a QueryStatement
	 * @param stmt
	 * @return
	 */
	public static BasicQuery createBasicQuery(QueryStatement stmt){
		BasicQuery basic = new BasicQuery();
		basic.setFrom(stmt.getTableName());
		basic.setSort(stmt.getSortField());
		basic.setAscending(stmt.getSortAcending());
		basic.setLimit(stmt.getLimit());
		basic.setOffset(stmt.getOffset()-1);
		basic.setFilters(stmt.getSearchCondition());
		basic.setSelect(stmt.getSelect());
		return basic;
	}

}
