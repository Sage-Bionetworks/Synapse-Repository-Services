package org.sagebionetworks;

public class ImmutablePropertyAccessor<T> implements PropertyAccessor<T> {
	private final T value;

	public ImmutablePropertyAccessor(T value) {
		this.value = value;
	}

	@Override
	public T get() {
		return value;
	}

	public static <T> ImmutablePropertyAccessor<T> create(T value) {
		return new ImmutablePropertyAccessor<T>(value);
	}
}
