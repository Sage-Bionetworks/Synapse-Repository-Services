package org.sagebionetworks.repo.manager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The unit test for EntityQueryManagerImpl.
 * @author John
 *
 */
public class EntityQueryManagerImplTest {

	NodeQueryDao mockDao;
	EntityQueryManagerImpl manager;
	NodeQueryResults sampleResutls;
	
	@Before
	public void before(){
		mockDao = Mockito.mock(NodeQueryDao.class);
		manager = new EntityQueryManagerImpl();
		ReflectionTestUtils.setField(manager, "NodeQueryDao", mockDao);
		
		sampleResutls = new NodeQueryResults();
		
		
	}
	
	@Test
	public void testHappy(){
		
	}
}
