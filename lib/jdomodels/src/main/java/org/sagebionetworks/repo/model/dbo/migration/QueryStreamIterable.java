package org.sagebionetworks.repo.model.dbo.migration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Provides generic support for iterating over query results one page at a time.
 *
 * @param <T>
 */
public class QueryStreamIterable<T> implements Iterable<T>, Iterator<T> {

	public static final String KEY_OFFSET = "KEY_OFFSET";
	public static final String KEY_LIMIT = "KEY_LIMIT";
	public static final String PAGINATION = " LIMIT :" + KEY_LIMIT + " OFFSET :" + KEY_OFFSET;

	NamedParameterJdbcTemplate namedTemplate;
	RowMapper<T> rowMapper;
	String sql;
	Map<String, Object> parameters;
	long limit;
	long offset;
	Iterator<T> currentPage;

	/**
	 * 
	 * @param namedTemplate
	 * @param rowMapper
	 * @param sql
	 * @param parameters
	 * @param limit
	 *            The limit sets the page size. This stream will never keep more
	 *            than one page of data in memory at a time.
	 */
	public QueryStreamIterable(NamedParameterJdbcTemplate namedTemplate, RowMapper<T> rowMapper, String sql,
			Map<String, Object> parameters, long limit) {
		super();
		this.namedTemplate = namedTemplate;
		this.rowMapper = rowMapper;
		StringBuilder sqlBuilder = new StringBuilder(sql);
		sqlBuilder.append(PAGINATION);
		this.sql = sqlBuilder.toString();
		this.limit = limit;
		this.offset = 0L;
		this.currentPage = null;
		this.parameters = new HashMap<>(parameters);
		this.parameters.put(KEY_LIMIT, this.limit);
		this.parameters.put(KEY_OFFSET, this.offset);
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		if (currentPage != null) {
			if (currentPage.hasNext()) {
				return true;
			}
		}
		// Use the current offset
		this.parameters.put(KEY_OFFSET, this.offset);
		// Nothing in the current page so fetch the next
		currentPage = namedTemplate.query(sql, parameters, rowMapper).iterator();
		// Bump pagination for the next page
		this.offset = this.offset + this.limit;
		// return the results for the current page.
		return currentPage.hasNext();
	}

	@Override
	public T next() {
		return currentPage.next();
	}
}
