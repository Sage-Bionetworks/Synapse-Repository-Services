package org.sagebionetworks.repo.model.dbo.dao.discussion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionSearchIndexDaoImpl implements DiscussionSearchIndexDao {

	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public DiscussionSearchIndexDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
