package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class ConnectionFactoryImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	
	@Before
	public void before(){

	}
	
	@Test
	public void testGetConnection(){
		assertNotNull(tableConnectionFactory);
		IdAndVersion idAndVersion = IdAndVersion.parse("123");
		// Validate that we can get a connection.
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		assertNotNull(indexDao);
		// Validate that we can use the connection to run a basic query.
		long one = indexDao.getConnection().queryForObject("SELECT 1", Long.class);
		assertEquals(1L, one);
	}
}
