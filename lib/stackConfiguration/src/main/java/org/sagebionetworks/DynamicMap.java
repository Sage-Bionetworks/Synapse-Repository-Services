package org.sagebionetworks;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Map that returns a calculated result for every key. Used for having spring bean keys of the form "something[string]"
 * where "string" is the key that is used to calculate a value
 */
public abstract class DynamicMap<K, V> implements Map<K, V> {

	protected abstract V create(Object key);

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		return true;
	}

	@Override
	public boolean containsValue(Object value) {
		return true;
	}

	@Override
	public V get(Object key) {
		return create(key);
	}

	@Override
	public V put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
