package org.sagebionetworks.repo.manager.table;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TableViewTruthManagerImplTest {
	
	@Mock
	NodeDAO mockNodeDao;
	@Mock
	ViewScopeDao mockViewScopeDao;

	TableViewTruthManagerImpl manager;
	
	String viewId;
	Long viewIdLong;
	
	Set<Long> scope;
	Set<Long> containersInScope;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new TableViewTruthManagerImpl();
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(manager, "viewScopeDao", mockViewScopeDao);
		
		viewId = "syn123";
		viewIdLong = KeyFactory.stringToKey(viewId);
		
		// setup the view scope
		Set<Long> scope = Sets.newHashSet(222L,333L);
		when(mockViewScopeDao.getViewScope(viewIdLong)).thenReturn(scope);
		when(mockNodeDao.getAllContainerIds(222L)).thenReturn(Lists.newArrayList(20L,21L));
		when(mockNodeDao.getAllContainerIds(333L)).thenReturn(Lists.newArrayList(30L,31L));
		
		containersInScope = Sets.newHashSet(222L,333L,20L,21L,30L,31L);
	}
	
	@Test
	public void testGetAllContainerIdsForViewScope(){
		// call under test.
		Set<Long> containers = manager.getAllContainerIdsForViewScope(viewId);
		assertEquals(containersInScope, containers);
	}
}
