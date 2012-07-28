package org.sagebionetworks.repo.manager.backup.migration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.MigratableDAO;
import org.sagebionetworks.repo.model.ObjectData;
import org.sagebionetworks.repo.model.ObjectDescriptor;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.jdo.ObjectDescriptorUtils;


public class DependencyManagerImplTest {
	
	
	private static final long LIST_SIZE = 3;
	
	private static QueryResults<ObjectData> generateMigrationData(long startId, long num, boolean isEntity) {
		QueryResults<ObjectData> qr = new QueryResults<ObjectData>();
		qr.setTotalNumberOfResults((int)LIST_SIZE);
		List<ObjectData> results = new ArrayList<ObjectData>();
		for (int i=0; i<num; i++) {
			ObjectData od = new ObjectData();
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
		QueryResults<ObjectData> results = dependencyManager.getAllObjects(0, requestedNum, true);
		
		List<ObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 3 from dao1 and 2 from dao2
		for (int i=0; i<3L; i++) {
			ObjectData od = ods.get(i);
			ObjectDescriptor id = od.getId();
			assertEquals(Entity.class.getName(), id.getType());
			assertEquals("syn"+i, id.getId());
		}
		for (int i=3; i<5L; i++) {
			ObjectData od = ods.get(i);
			ObjectDescriptor id = od.getId();
			assertEquals(UserGroup.class.getName(), id.getType());
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
		QueryResults<ObjectData> results = dependencyManager.getAllObjects(3, requestedNum, true);
		
		List<ObjectData> ods = results.getResults();
		assertEquals(requestedNum, ods.size());
		assertEquals(2L*LIST_SIZE, results.getTotalNumberOfResults());
		
		// should get 2 from dao2
		for (int i=3; i<5L; i++) {
			ObjectData od = ods.get(i-3);
			ObjectDescriptor id = od.getId();
			assertEquals(UserGroup.class.getName(), id.getType());
			assertEquals(""+i, id.getId());
		}
	}
}
