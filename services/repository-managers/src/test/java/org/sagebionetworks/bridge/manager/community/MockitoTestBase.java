package org.sagebionetworks.bridge.manager.community;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.listeners.CollectCreatedMocks;
import org.mockito.internal.progress.ThreadSafeMockingProgress;

import com.google.common.collect.Lists;

public class MockitoTestBase {
	private List<Object> createdMocks = null;

	/**
	 * if necessary, this can be called in the subclass @Before to initialize the mocks earlier than in the @Before of
	 * the superclass, which happens later
	 */
	protected void initMockito() {
		if (createdMocks == null) {
			createdMocks = Lists.newArrayList();
			new ThreadSafeMockingProgress().setListener(new CollectCreatedMocks(createdMocks));
			MockitoAnnotations.initMocks(this);
		}
	}

	@Before
	public void doInitMockito() {
		initMockito();
	}

	@After
	public void verifyMockito() {
		Mockito.validateMockitoUsage();
		Mockito.verifyNoMoreInteractions(createdMocks.toArray());
	}
}
