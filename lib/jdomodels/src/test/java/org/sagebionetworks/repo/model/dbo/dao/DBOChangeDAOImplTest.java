package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChangeDAOImplTest {
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Test
	public void testReplace(){
		DBOChange change = new DBOChange();
		change.setObjectId(new Long(123));
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		change = changeDAO.replaceChange(change);
		assertNotNull(change);
		assertNotNull(change.getChangeNumber());
	}

}
