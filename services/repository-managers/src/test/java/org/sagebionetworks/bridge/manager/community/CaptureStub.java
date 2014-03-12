package org.sagebionetworks.bridge.manager.community;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Function;

public class CaptureStub<T> {
	private final ArgumentCaptor<T> value;
	private final Function<T, T> convertor;

	public static <T> CaptureStub<T> forClass(Class<T> klass) {
		return forClass(klass, null);
	}

	public static <T> CaptureStub<T> forClass(Class<T> klass, Function<T, T> convertor) {
		return new CaptureStub<T>(klass, convertor);
	}

	private CaptureStub(Class<T> klass, Function<T, T> convertor) {
		value = ArgumentCaptor.forClass(klass);
		this.convertor = convertor;
	}

	public T capture() {
		return value.capture();
	}

	public Answer<T> answer() {
		return new Answer<T>() {
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				return convert(value.getValue());
			}
		};
	}

	private T convert(T t) {
		if (convertor != null) {
			t = convertor.apply(t);
		}
		return t;
	}
}
