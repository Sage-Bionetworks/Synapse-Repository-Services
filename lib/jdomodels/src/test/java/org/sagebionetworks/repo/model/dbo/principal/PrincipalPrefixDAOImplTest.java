package org.sagebionetworks.repo.model.dbo.principal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dbo.dao.PrincipalPrefixDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalPrefixDAOImplTest {

	@Autowired
	PrincipalPrefixDAO principalPrefixDao;
	
	@Test
	public void testPreProcessToken(){
		
	}
}
