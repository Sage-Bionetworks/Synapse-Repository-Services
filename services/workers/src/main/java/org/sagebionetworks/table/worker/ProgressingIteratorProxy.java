package org.sagebionetworks.table.worker;

import java.util.Iterator;

import org.sagebionetworks.repo.model.table.Row;

/**
 * A simple proxy for reporting progress on an iterator.
 * 
 * @author John
 *
 */
public class ProgressingIteratorProxy implements Iterator<Row> {

	private Iterator<Row> wrappedIterator;
	ProgressReporter reporter;
	private int rowCount = 0;
	
	/**
	 * Create a new object for each use.
	 * 
	 * @param wrappedIterator The real iterator.
	 * @param reporter The progress reporter.
	 */
	public ProgressingIteratorProxy(Iterator<Row> wrappedIterator,
			ProgressReporter reporter) {
		super();
		this.wrappedIterator = wrappedIterator;
		this.reporter = reporter;
	}

	@Override
	public boolean hasNext() {
		return wrappedIterator.hasNext();
	}

	@Override
	public Row next() {
		Row row = wrappedIterator.next();
		reporter.tryReportProgress(rowCount);
		rowCount++;
		return row;
	}

	@Override
	public void remove() {
		wrappedIterator.remove();
	}

}
