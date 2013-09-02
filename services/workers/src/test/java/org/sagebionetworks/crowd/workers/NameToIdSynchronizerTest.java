package org.sagebionetworks.crowd.workers;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.NamedIdGenerator;
import org.sagebionetworks.ids.NamedIdGenerator.NamedType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class NameToIdSynchronizerTest {

	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NamedIdGenerator namedIdGenerator;
	@Autowired
	private NameToIdSynchronizer nameToIdSynchronizer;
	
	@Test
	public void testSynch(){
		// Let the worker do its thing
		nameToIdSynchronizer.run();
		// Now check the results
		Collection<UserGroup> all = userGroupDAO.getAll();
		for(UserGroup ug: all){
			long id = namedIdGenerator.generateNewId(ug.getName(), NamedType.USER_GROUP_ID);
			// Skip the BOOTSTRAP_USER_GROUP as it is assigned to zero;
			if(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME.equals(ug.getName())){
				System.out.println("skipping the BOOTSTRAP_USER_GROUP because its ID is zero and that is not supportable by an auto-generated table");
				continue;
			}
			assertEquals("The NameToIdSynchronizer should ensure that the user's name is assigned to its ID for the NamedIdGenerator for: "+ug.getName(),Long.parseLong(ug.getId()), id);
		}
	}
}
