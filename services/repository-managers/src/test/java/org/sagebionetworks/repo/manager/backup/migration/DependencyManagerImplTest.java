package org.sagebionetworks.repo.manager.backup.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.jdo.ObjectDescriptorUtils;


public class DependencyManagerImplTest {
	
	
	private static final long LIST_SIZE = 3;
	
	private static QueryResults<MigratableObjectData> generateMigrationData(long startId, long num, MigratableObjectType mot) {
		QueryResults<MigratableObjectData> qr = new QueryResults<MigratableObjectData>();
		qr.setTotalNumberOfResults((int)LIST_SIZE);
		List<MigratableObjectData> results = new ArrayList<MigratableObjectData>();
		for (int i=0; i<num; i++) {
			MigratableObjectData od = new MigratableObjectData();
			if (MigratableObjectType.ENTITY == mot) {
				od.setId(ObjectDescriptorUtils.createEntityObjectDescriptor(startId++));
			} else  if (MigratableObjectType.PRINCIPAL == mot){
				od.setId(ObjectDescriptorUtils.createPrincipalObjectDescriptor(startId++));			
			} else {
				od.setId(ObjectDescriptorUtils.createAccessRequirementObjectDescriptor(startId++));
			}
			results.add(od);
		}
		qr.setResults(results);
		return qr;
	}

	private static QueryResults<MigratableObjectCount> generateMigratableObjectCounts(long startId, long num, MigratableObjectType mot) {
		QueryResults<MigratableObjectCount> r = new QueryResults<MigratableObjectCount>();
		List<MigratableObjectCount> l = new ArrayList<MigratableObjectCount>();
		MigratableObjectCount oc = new MigratableObjectCount();
		oc.setObjectType(mot.name());
		oc.setEntityType(null);
		oc.setCount(num);
		l.add(oc);
		r.setTotalNumberOfResults(1);
		r.setResults(l);
		return r;
	}
	
	/**
	 * gets one page spanning the entire content of all the DAOs
	 */
	@Test
	public void testGetAllObjectsZeroOffset() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(anyLong()/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 3L, MigratableObjectType.ENTITY));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(2L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(3L, 2L, MigratableObjectType.PRINCIPAL));
		migratableDaos.add(dao2);
		

		dependencyManager.setMigratableDaos(migratableDaos);
		int requestedNum = 5;
		QueryResults<MigratableObjectData> results = dependencyManager.getAllObjects(0, requestedNum, true);
		
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 3 from dao1 and 2 from dao2
		for (int i=0; i<3L; i++) {
			MigratableObjectData od = ods.get(i);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.ENTITY, id.getType());
			assertEquals("syn"+i, id.getId());
		}
		for (int i=3; i<5L; i++) {
			MigratableObjectData od = ods.get(i);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.PRINCIPAL, id.getType());
			assertEquals(""+i, id.getId());
		}
	}

	/**
	 * gets one page spanning twos DAOs, but will just some of the content
	 */
	@Test
	public void testGetSomeObjectsNonZeroOffsetOverlapDAOs() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		// return IDs 1,2 (not 0)
		when(dao1.getMigrationObjectData(anyLong()/*offset*/, eq(3L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(1L, 2L, MigratableObjectType.ENTITY));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		// return ID 3 (not 4)
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(1L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(3L, 1L, MigratableObjectType.PRINCIPAL));
		migratableDaos.add(dao2);
		

		dependencyManager.setMigratableDaos(migratableDaos);
		int offset = 1;
		int requestedNum = 3;
		QueryResults<MigratableObjectData> results = dependencyManager.getAllObjects(offset, requestedNum, true);
		
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 2 from dao1 and 1 from dao2
		for (int i=offset; i<2L; i++) {
			MigratableObjectData od = ods.get(i-offset);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.ENTITY, id.getType());
			assertEquals("syn"+i, id.getId());
		}
		for (int i=3; i<4L; i++) {
			MigratableObjectData od = ods.get(i-1);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.PRINCIPAL, id.getType());
			assertEquals(""+i, id.getId());
		}
	}

	/**
	 * Gets the entire content of DAO #2, starting from its beginning
	 * 
	 */
	@Test
	public void testGetAllObjectsNonZeroOffset() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(eq(3L)/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 0L, MigratableObjectType.ENTITY));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(2L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(3L, 2L, MigratableObjectType.PRINCIPAL));
		migratableDaos.add(dao2);
		

		dependencyManager.setMigratableDaos(migratableDaos);
		int requestedNum = 2;
		QueryResults<MigratableObjectData> results = dependencyManager.getAllObjects(3, requestedNum, true);
		
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 2 from dao2
		for (int i=3; i<5L; i++) {
			MigratableObjectData od = ods.get(i-3);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.PRINCIPAL, id.getType());
			assertEquals(""+i, id.getId());
		}
	}
	/**
	 * Gets the partial content of DAO #2, starting from a non-zero offset in DAO #2
	 * 
	 */
	@Test
	public void testGetSomeObjectsFromDAO2NonZeroOffset() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(eq(4L)/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 0L, MigratableObjectType.ENTITY));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(1L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(4L, 1L, MigratableObjectType.PRINCIPAL));
		migratableDaos.add(dao2);
		

		dependencyManager.setMigratableDaos(migratableDaos);
		int requestedNum = 1;
		int startIndex = 4;
		QueryResults<MigratableObjectData> results = dependencyManager.getAllObjects(startIndex, requestedNum, true);
		
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 2 from dao2
		for (int i=startIndex; i<5L; i++) {
			MigratableObjectData od = ods.get(i-startIndex);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.PRINCIPAL, id.getType());
			assertEquals(""+i, id.getId());
		}
	}

	
	@Ignore
	@Test
	public void testGetAllObjectsCounts() throws Exception {
		DependencyManagerImpl dependencyMgr = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(eq(4L)/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 0L, MigratableObjectType.ENTITY));
		migratableDaos.add(dao1);
				
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(1L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(4L, 1L, MigratableObjectType.PRINCIPAL));
		migratableDaos.add(dao2);
		
		dependencyMgr.setMigratableDaos(migratableDaos);
		QueryResults<MigratableObjectCount> results = dependencyMgr.getAllObjectsCounts(0, 100, true);
		assertEquals(2, results.getTotalNumberOfResults());
		assertEquals(2, results.getResults().size());
	}
}
