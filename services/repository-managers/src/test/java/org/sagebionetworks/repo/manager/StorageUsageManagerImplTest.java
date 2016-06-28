package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.StorageUsageQueryDao;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

@Ignore
public class StorageUsageManagerImplTest {

	private Long userId = 0L;
	
	@Test
	public void testGetStorageUsage() throws Exception {

		int offset = 1;
		int limit = 2;
		long total = 100;

		// Mock
		List<StorageUsage> storageList = new ArrayList<StorageUsage>();
		StorageUsage s1 = Mockito.mock(StorageUsage.class);
		storageList.add(s1);
		StorageUsage s2 = Mockito.mock(StorageUsage.class);
		storageList.add(s2);

		StorageUsageQueryDao mockDao = Mockito.mock(StorageUsageQueryDao.class);
		Mockito.when(mockDao.getUsageInRangeForUser(userId, offset, offset + limit)).thenReturn(storageList);
		Mockito.when(mockDao.getTotalCountForUser(userId)).thenReturn(total);

		StorageUsageManager man = new StorageUsageManagerImpl();
		man = unwrap(man);
		ReflectionTestUtils.setField(man, "storageUsageDao", mockDao);

		// Test
		QueryResults<StorageUsage> results = man.getUsageInRangeForUser(userId, offset, limit);
		Assert.assertEquals(total, results.getTotalNumberOfResults());
		Assert.assertEquals(storageList.size(), results.getResults().size());
		Assert.assertEquals(s1, results.getResults().get(0));
		Assert.assertEquals(s2, results.getResults().get(1));
		Mockito.verify(mockDao, Mockito.times(1)).getUsageInRangeForUser(userId, offset, offset + limit);
	}

	private StorageUsageManager unwrap(StorageUsageManager man) throws Exception {
		if(AopUtils.isAopProxy(man) && man instanceof Advised) {
			Object target = ((Advised)man).getTargetSource().getTarget();
			return (StorageUsageManager)target;
		}
		return man;
	}
}
