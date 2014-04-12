package org.sagebionetworks.repo.model.dbo.dao.table;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.table.AsynchTableJobStatusDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AsynchTableJobStatusDaoImplTest {
	
	@Autowired
	AsynchTableJobStatusDAO asynchTableJobStatusDao;
	
	@Test
	public void test(){
		
	}
}
