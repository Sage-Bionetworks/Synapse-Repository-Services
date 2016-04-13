package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ViewScopeDaoImplTest {

	@Autowired
	ViewScopeDao viewScopeDao;
	
	@After
	public void after(){
		viewScopeDao.truncateAll();
	}
	
	@Test
	public void testViewScopeHappy(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScope(viewId1, containers);
		// find the intersection
		Set<Long> intersection = viewScopeDao.findViewScopeIntersectionWithPath(containers);
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));		
	}
	
	@Test
	public void testViewScopeUpdate(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScope(viewId1, containers);
		// change the values
		containers = Sets.newHashSet(555L,777L);
		viewScopeDao.setViewScope(viewId1, containers);
		// The 444 container should no longer intersect.
		Set<Long> intersection = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(444L));
		assertNotNull(intersection);
		assertEquals(0, intersection.size());
		// should intersect with 555
		intersection = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(555L));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));
		// should intersect with 777
		intersection = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(777L));
		assertNotNull(intersection);
		assertEquals(1, intersection.size());
		assertTrue(intersection.contains(viewId1));		
	}
	
	@Test
	public void testViewScopeMultipleOverlap(){
		long viewId1 = 123L;
		long viewId2 = 456L;
		// one
		viewScopeDao.setViewScope(viewId1, Sets.newHashSet(444L,555L));
		// two
		viewScopeDao.setViewScope(viewId2, Sets.newHashSet(555L,888L));
		// 555 should match both
		Set<Long> results = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(555L));
		assertEquals(Sets.newHashSet(viewId1, viewId2), results);
		// 444 should match one
		results = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(444L));
		assertEquals(Sets.newHashSet(viewId1), results);
		// 888 should match one
		results = viewScopeDao.findViewScopeIntersectionWithPath(Sets.newHashSet(888L));
		assertEquals(Sets.newHashSet(viewId2), results);
		
	}
}
