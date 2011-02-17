package org.sagebionetworks.web.util;

import org.mockito.Mockito;

import com.gwtplatform.tester.MockFactory;

public class MockitoMockFactory  implements MockFactory {

	@Override
	public <T> T mock(Class<T> classToMock) {
		// We use Mockito to create all stubs.
		return Mockito.mock(classToMock);
	}

}
