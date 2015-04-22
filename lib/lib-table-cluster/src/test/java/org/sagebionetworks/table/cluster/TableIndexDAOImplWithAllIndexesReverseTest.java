package org.sagebionetworks.table.cluster;

import static org.mockito.Mockito.stub;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.sagebionetworks.ImmutablePropertyAccessor;
import org.sagebionetworks.StackConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

public class TableIndexDAOImplWithAllIndexesReverseTest extends TableIndexDAOImplTest {

	private StackConfiguration oldStackConfiguration = null;

	@Before
	public void setupStackConfig() {
		oldStackConfiguration = StackConfiguration.singleton();
		StackConfiguration mockedStackConfiguration = Mockito.spy(oldStackConfiguration);
		stub(mockedStackConfiguration.getTableAllIndexedEnabled()).toReturn(
				new ImmutablePropertyAccessor<Boolean>(!oldStackConfiguration.getTableAllIndexedEnabled().get()));
		ReflectionTestUtils.setField(StackConfiguration.singleton(), "singleton", mockedStackConfiguration);
	}

	@After
	public void teardownStackConfig() {
		if (oldStackConfiguration != null) {
			ReflectionTestUtils.setField(StackConfiguration.singleton(), "singleton", oldStackConfiguration);
		}
	}
}
