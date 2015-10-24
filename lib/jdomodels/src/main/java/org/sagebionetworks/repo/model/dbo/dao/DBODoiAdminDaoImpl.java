package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOI;

import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class DBODoiAdminDaoImpl implements DoiAdminDao {

	private static final String DELETE_ALL = "DELETE FROM " + TABLE_DOI;

	@Autowired private SimpleJdbcTemplate simpleJdbcTemplate;

	@WriteTransaction
	@Override
	public void clear() {
		simpleJdbcTemplate.update(DELETE_ALL);
	}
}
