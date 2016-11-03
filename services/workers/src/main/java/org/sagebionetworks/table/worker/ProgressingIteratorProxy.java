package org.sagebionetworks.table.worker;

import java.util.Iterator;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.table.SparseRowDto;

/**
 * A simple proxy for reporting progress on an iterator.
 * 
 * @author John
 *
 */
public class ProgressingIteratorProxy implements Iterator<SparseRowDto> {

	private Iterator<SparseRowDto> wrappedIterator;
	ProgressCallback<Integer> progressCallback;
	private int rowCount = 0;
	
	/**
	 * Create a new object for each use.
	 * 
	 * @param wrappedIterator The real iterator.
	 * @param reporter The progress reporter.
	 */
	public ProgressingIteratorProxy(Iterator<SparseRowDto> wrappedIterator,
			ProgressCallback<Integer> progressCallback) {
		super();
		this.wrappedIterator = wrappedIterator;
		this.progressCallback = progressCallback;
	}

	@Override
	public boolean hasNext() {
		return wrappedIterator.hasNext();
	}

	@Override
	public SparseRowDto next() {
		SparseRowDto row = wrappedIterator.next();
		progressCallback.progressMade(rowCount);
		rowCount++;
		return row;
	}

	@Override
	public void remove() {
		wrappedIterator.remove();
	}

}
