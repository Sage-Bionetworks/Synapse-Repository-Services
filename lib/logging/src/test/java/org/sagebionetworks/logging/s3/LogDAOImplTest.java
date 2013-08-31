package org.sagebionetworks.logging.s3;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:log-sweeper.spb.xml" })
public class LogDAOImplTest {
	
	@Autowired
	LogDAO logDAO;

	@Before
	public void before(){
		
	}
}
