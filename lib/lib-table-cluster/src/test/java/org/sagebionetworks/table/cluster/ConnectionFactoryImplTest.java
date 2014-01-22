package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-spb.xml" })
public class ConnectionFactoryImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	
	@Test
	public void test(){
		assertNotNull(tableConnectionFactory);
	}
}
