package org.sagebionetworks.spring.transaction;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StreamingJdbcTemplateTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	public void testStreamingJdbcTemplate() {
		assertTrue(jdbcTemplate instanceof StreamingJdbcTemplate);
	}

	@Test
	public void testStreamingConfiguration() {
		// Use a StatementCallback to gain access to how the Statement is
		// configured.
		jdbcTemplate.execute(new StatementCallback<Void>() {

			@Override
			public Void doInStatement(Statement stmt) throws SQLException,
					DataAccessException {
				/*
				 * Three conditions are required for RowSet streaming in MySQL.
				 * 
				 * See:
				 * http://dev.mysql.com/doc/connector-j/en/connector-j-reference
				 * -implementation-notes.html
				 */
				assertEquals(
						"For RowSet streaming in MySQL, the fetch size must be Integer.MIN_VALUE",
						Integer.MIN_VALUE, stmt.getFetchSize());
				assertEquals(
						"For RowSet streaming in MySQL, the result set type must be: java.sql.ResultSet.TYPE_FORWARD_ONLY",
						java.sql.ResultSet.TYPE_FORWARD_ONLY,
						stmt.getResultSetType());
				assertEquals(
						"For RowSet streaming in MySQL, the result set concurrency must be: java.sql.ResultSet.CONCUR_READ_ONLY",
						java.sql.ResultSet.CONCUR_READ_ONLY,
						stmt.getResultSetConcurrency());

				return null;
			}
		});
	}

}
