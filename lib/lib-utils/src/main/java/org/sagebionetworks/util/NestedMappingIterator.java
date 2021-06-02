package org.sagebionetworks.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Implementation of an iterator that given an iterator applies a mapping function to extract a
 * nested iterator for each element and produce an flattened iterator of such transformation. This
 * can be used in cases where iterating over a list would need to expand such iteration into a
 * nested list of elements.
 * 
 * @author Marco Marasca
 *
 */
public class NestedMappingIterator<T, R> implements Iterator<R> {

	private Iterator<T> inputIterator;
	private Function<T, Iterator<R>> mappingIteratorProvider;

	private Iterator<R> currentIterator;

	/**
	 * 
	 * @param inputIterator           The input iterator
	 * @param mappingIteratorProvider A function that given an element in the input iterator returns
	 *                                another iterator
	 */
	public NestedMappingIterator(Iterator<T> inputIterator, Function<T, Iterator<R>> mappingIteratorProvider) {
		this.inputIterator = inputIterator;
		this.mappingIteratorProvider = mappingIteratorProvider;
	}

	@Override
	public boolean hasNext() {
		// We reached the end of the current iterator, reset the iterator
		if (currentIterator != null && !currentIterator.hasNext()) {
			currentIterator = null;
		}

		if (currentIterator == null) {
			// Move to the first non empty iterator
			while (inputIterator.hasNext()) {
				currentIterator = mappingIteratorProvider.apply(inputIterator.next());
				if (currentIterator != null && currentIterator.hasNext()) {
					return true;
				}
			}
		}

		return currentIterator != null && currentIterator.hasNext();
	}

	@Override
	public R next() {
		if (currentIterator == null) {
			throw new IllegalStateException("hasNext() must be called before next()");
		}
		return currentIterator.next();
	}

}
