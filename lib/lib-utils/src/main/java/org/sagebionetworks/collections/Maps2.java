package org.sagebionetworks.collections;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Supplier;

public class Maps2 {
	@SuppressWarnings("serial")
	public static <K, V> Map<K, V> createSupplierHashMap(final Supplier<V> supplier) {
		return new HashMap<K, V>() {
			@SuppressWarnings("unchecked")
			@Override
			public V get(Object key) {
				V value = super.get(key);
				if (value == null) {
					value = supplier.get();
					super.put((K) key, value);
				}
				return value;
			}
		};
	};
}
