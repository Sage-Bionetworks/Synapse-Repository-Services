package org.sagebionetworks.repo.manager.entity;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class ReplicationMessageManagerAsynchImplTest {
	
	@Mock
	ExecutorService mockThreadPool;
	
	@Mock
	ReplicationMessageManager mockReplicationMessageManager;
	
	@Mock
	Future<Void> mockFuture;
	
	@InjectMocks
	ReplicationMessageManagerAsynchImpl manager;
	
	@Before
	public void before() {
		when(mockThreadPool.submit(any(Callable.class))).thenReturn(mockFuture);
	}
	
	@Test
	public void testPushContainerIdsToReconciliationQueue() {
		List<Long> scopeIds = Lists.newArrayList(123L);
		// call under test
		Future<Void> future = manager.pushContainerIdsToReconciliationQueue(scopeIds);
		assertEquals(mockFuture, future);
		verify(mockThreadPool).submit(any(Callable.class));
	}

}
