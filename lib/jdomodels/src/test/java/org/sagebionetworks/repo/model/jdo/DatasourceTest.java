package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.database.StreamingJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DatasourceTest {
	
	@Autowired
	BasicDataSource dataSourcePool;
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Test
	public void testTransactionIsolation() throws SQLException{
		Connection con = dataSourcePool.getConnection();
		try{
			assertEquals(java.sql.Connection.TRANSACTION_READ_COMMITTED, con.getTransactionIsolation());
		}finally{
			con.close();
		}
	}
	
	@Test
	public void testStreamingTempalte(){
		assertTrue("The JdbcTempalte should be a StreamingJdbcTemplate",jdbcTemplate instanceof StreamingJdbcTemplate);
	}

}
