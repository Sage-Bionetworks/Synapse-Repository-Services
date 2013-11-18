package org.sagebionetworks.bridge.manager.community;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CaptureStub<T> {
	final ArgumentCaptor<T> value;

	public static <T> CaptureStub<T> forClass(Class<T> klass) {
		return new CaptureStub<T>(klass);
	}

	private CaptureStub(Class<T> klass) {
		value = ArgumentCaptor.forClass(klass);
	}

	public T capture() {
		return value.capture();
	}

	public Answer<T> answer() {
		return new Answer<T>() {
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				return value.getValue();
			}
		};
	}
}
