package org.sagebionetworks.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
}
