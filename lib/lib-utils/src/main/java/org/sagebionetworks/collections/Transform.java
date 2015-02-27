package org.sagebionetworks.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Transform {
	public static <F, T> List<T> toList(Iterable<F> iterable, Function<F, T> transformer) {
		List<T> result;
		if (iterable instanceof Collection<?>) {
			int size = ((Collection<?>) iterable).size();
			result = Lists.newArrayListWithCapacity(size);
		} else {
			result = Lists.newLinkedList();
		}
		for (F o : iterable) {
			result.add(transformer.apply(o));
		}
		return result;
	}

	public static <F, T> Set<T> toSet(Iterable<F> iterable, Function<F, T> transformer) {
		Set<T> result;
		if (iterable instanceof Collection<?>) {
			int size = ((Collection<?>) iterable).size();
			result = Sets.newHashSetWithExpectedSize(size);
		} else {
			result = Sets.newHashSet();
		}
		for (F o : iterable) {
			result.add(transformer.apply(o));
		}
		return result;
	}

	public static class TransformEntry<K, V> {
		final K key;
		final V value;

		public TransformEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
	}

	public static <F, TK, TV> Map<TK, TV> toMap(Iterable<F> iterable, Function<F, TransformEntry<TK, TV>> transformer) {
		Map<TK, TV> result = Maps.newHashMap();
		for (F o : iterable) {
			TransformEntry<TK, TV> entry = transformer.apply(o);
			result.put(entry.key, entry.value);
		}
		return result;
	}

	public static <K, V> Map<K, V> toIdMap(Iterable<V> iterable, Function<V, K> transformer) {
		Map<K, V> result = Maps.newHashMap();
		for (V value : iterable) {
			K key = transformer.apply(value);
			result.put(key, value);
		}
		return result;
	}

	public static <K, V> LinkedHashMap<K, V> toOrderedIdMap(Iterable<V> iterable, Function<V, K> transformer) {
		LinkedHashMap<K, V> result = Maps.newLinkedHashMap();
		for (V value : iterable) {
			K key = transformer.apply(value);
			result.put(key, value);
		}
		return result;
	}

	public static <T, F> Iterable<T> castElements(final Iterable<?> iterable) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				// TODO Auto-generated method stub
				return new Iterator<T>() {
					Iterator<?> iterator = iterable.iterator();

					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@SuppressWarnings("unchecked")
					@Override
					public T next() {
						return (T) iterator.next();
					}

					@Override
					public void remove() {
						iterator.remove();
					}
				};
			}
		};
	}

	public static <K, T> List<T> transformKeysToObjects(Collection<K> keys, Map<K, T> transformMap, boolean failOnNotInMap) {
		List<T> result = Lists.newArrayListWithCapacity(keys.size());
		for (K key : keys) {
			T obj = transformMap.get(key);
			if (obj != null) {
				result.add(obj);
			} else if (failOnNotInMap) {
				throw new IllegalArgumentException("No entry found for " + key);
			}
		}
		return result;
	}
}
