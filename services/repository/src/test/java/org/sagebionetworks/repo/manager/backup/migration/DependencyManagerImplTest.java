package org.sagebionetworks.repo.manager.backup.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.ObjectDescriptorUtils;


public class DependencyManagerImplTest {
	
	
	private static final long LIST_SIZE = 3;
	
	private static QueryResults<MigratableObjectData> generateMigrationData(long startId, long num, boolean isEntity) {
		QueryResults<MigratableObjectData> qr = new QueryResults<MigratableObjectData>();
		qr.setTotalNumberOfResults((int)LIST_SIZE);
		List<MigratableObjectData> results = new ArrayList<MigratableObjectData>();
		for (int i=0; i<num; i++) {
			MigratableObjectData od = new MigratableObjectData();
			if (isEntity) {
				od.setId(ObjectDescriptorUtils.createEntityObjectDescriptor(startId++));
			} else {
				od.setId(ObjectDescriptorUtils.createPrincipalObjectDescriptor(startId++));			
			}
			results.add(od);
		}
		qr.setResults(results);
		return qr;
	}

	
	@Test
	public void testGetAllObjectsZeroOffset() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(anyLong()/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 3L, true));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(2L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(3L, 2L, false));
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
			assertEquals(MigratableObjectType.Entity, id.getType());
			assertEquals("syn"+i, id.getId());
		}
		for (int i=3; i<5L; i++) {
			MigratableObjectData od = ods.get(i);
			MigratableObjectDescriptor id = od.getId();
			assertEquals(MigratableObjectType.UserGroup, id.getType());
			assertEquals(""+i, id.getId());
		}
	}

	@Test
	public void testGetAllObjectsNonZeroOffset() throws Exception {
		DependencyManagerImpl dependencyManager = new DependencyManagerImpl();
		List<MigratableDAO> migratableDaos = new ArrayList<MigratableDAO>();
		
		MigratableDAO dao1 = Mockito.mock(MigratableDAO.class);
		when(dao1.getCount()).thenReturn(LIST_SIZE);
		when(dao1.getMigrationObjectData(eq(3L)/*offset*/, anyLong()/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(0L, 0L, true));
		migratableDaos.add(dao1);
		
		
		MigratableDAO dao2 = Mockito.mock(MigratableDAO.class);
		when(dao2.getCount()).thenReturn(LIST_SIZE);
		when(dao2.getMigrationObjectData(anyLong()/*offset*/, eq(2L)/*limit*/, anyBoolean()/*includeDependencies*/)).thenReturn(generateMigrationData(3L, 2L, false));
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
			assertEquals(MigratableObjectType.UserGroup, id.getType());
			assertEquals(""+i, id.getId());
		}
	}
}
