package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class NodeInheritanceManagerImplUnitTest {
	
	@Mock
	NodeInheritanceDAO nodeInheritanceDao;
	@Mock
	NodeDAO nodeDao;
	
	NodeInheritanceManagerImpl manager;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		manager = new NodeInheritanceManagerImpl();
		ReflectionTestUtils.setField(manager, "nodeInheritanceDao", nodeInheritanceDao);
		ReflectionTestUtils.setField(manager, "nodeDao", nodeDao);
	}
	
	/**
	 * PLFM-3713 was caused by a race condition where a folder was moved into
	 * a second folder that was being moved at the exact same time.  To
	 * prevent this we now lock both the original benefactor and new benefactor
	 * before rebuilding benefactors due to a hierarchy change.
	 */
	@Test
	public void testPLFM_3713(){
		String toMoveId = "123";
		String toMoveStartBenefactor = "456";
		
		String newParentId = "789";
		String newParentBenefactor = "901";
		
		when(nodeInheritanceDao.getBenefactorCached(toMoveId)).thenReturn(toMoveStartBenefactor);
		when(nodeInheritanceDao.getBenefactorCached(newParentId)).thenReturn(newParentBenefactor);
		
		// call under test
		manager.nodeParentChanged(toMoveId, newParentId, false);
		// Must lock on the node and parent before benefactor lookup.
		verify(nodeDao).lockNodes(Lists.newArrayList(toMoveId, newParentId));
		// start and end benefactor should be locked.
		verify(nodeDao).lockNodes(Lists.newArrayList(toMoveStartBenefactor, newParentBenefactor));
	}

}
