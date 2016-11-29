package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import org.sagebionetworks.repo.model.DoiAdminDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class DBODoiAdminDaoImpl implements DoiAdminDao {

	private static final String DELETE_ALL = "DELETE FROM " + TABLE_DOI;

	@Autowired private JdbcTemplate jdbcTemplate;

	@WriteTransaction
	@Override
	public void clear() {
		jdbcTemplate.update(DELETE_ALL);
	}
}
