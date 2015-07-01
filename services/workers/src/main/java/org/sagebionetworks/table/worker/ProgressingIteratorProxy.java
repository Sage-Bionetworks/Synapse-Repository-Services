package org.sagebionetworks.table.worker;

import java.util.Iterator;

import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

/**
 * A simple proxy for reporting progress on an iterator.
 * 
 * @author John
 *
 */
public class ProgressingIteratorProxy implements Iterator<Row> {

	private Iterator<Row> wrappedIterator;
	ProgressCallback<Integer> progressCallback;
	private int rowCount = 0;
	
	/**
	 * Create a new object for each use.
	 * 
	 * @param wrappedIterator The real iterator.
	 * @param reporter The progress reporter.
	 */
	public ProgressingIteratorProxy(Iterator<Row> wrappedIterator,
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
	public Row next() {
		Row row = wrappedIterator.next();
		progressCallback.progressMade(rowCount);
		rowCount++;
		return row;
	}

	@Override
	public void remove() {
		wrappedIterator.remove();
	}

}
