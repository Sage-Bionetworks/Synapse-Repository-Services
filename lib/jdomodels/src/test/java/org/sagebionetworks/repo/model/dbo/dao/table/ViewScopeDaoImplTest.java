package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ViewScopeDaoImplTest {

	@Autowired
	private ViewScopeDao viewScopeDao;
	@Autowired
	private DBOBasicDao basicDao;
	
	private ViewScopeType viewScopeType;
	
	@BeforeEach
	public void before() {
		viewScopeType = new ViewScopeType(ViewObjectType.ENTITY, ViewTypeMask.File.getMask());
	}
	
	@AfterEach
	public void after(){
		viewScopeDao.truncateAll();
	}
	
	@Test
	public void testSetViewScopeGetViewScoped(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		// find the intersection
		Set<Long> fetched = viewScopeDao.getViewScope(viewId1);
		assertEquals(containers, fetched);
		assertEquals(viewScopeType, viewScopeDao.getViewScopeType(viewId1));
	}
	
	
	@Test
	public void testSetViewTypeMaskNotFound(){
		long viewId1 = 123L;
		assertThrows(NotFoundException.class, ()->{
			// call under test
			viewScopeDao.getViewScopeType(viewId1);
		});
	}
	
	@Test
	public void testSetViewScopeUpdateEtagChange(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		
		// check the value in the database.
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("viewId", viewId1);
		DBOViewType dboType = basicDao.getObjectByPrimaryKey(DBOViewType.class, param);
		assertNotNull(dboType);
		assertEquals(new Long(viewId1), dboType.getViewId());
		assertNotNull(dboType.getEtag());
		assertEquals(null, dboType.getViewType());
		assertEquals(new Long(ViewTypeMask.File.getMask()), dboType.getViewTypeMask());
		String startEtag = dboType.getEtag();
		
		// update one
		containers = Sets.newHashSet(444L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		// check the etag
		dboType = basicDao.getObjectByPrimaryKey(DBOViewType.class, param);
		assertNotNull(dboType.getEtag());
		assertNotEquals(startEtag, dboType.getEtag());
	}
	
	@Test
	public void testViewScopeUpdate(){
		long viewId1 = 123L;
		Set<Long> containers = Sets.newHashSet(444L,555L);
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		
		// change the values
		containers = Sets.newHashSet(555L,777L);
		
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		
		Set<Long> result = viewScopeDao.getViewScope(viewId1);
		
		assertEquals(containers, result);
	}
	
	@Test
	public void testSetViewScopeNull(){
		long viewId1 = 123L;
		
		Set<Long> containers = Sets.newHashSet(444L,555L);
		
		// one
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);
		
		Set<Long> result = viewScopeDao.getViewScope(viewId1);
		
		assertEquals(containers, result);
		
		// set the scope null
		containers = null;
		
		viewScopeDao.setViewScopeAndType(viewId1, containers, viewScopeType);

		assertTrue(viewScopeDao.getViewScope(viewId1).isEmpty());
	}
	
}
