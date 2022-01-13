package org.sagebionetworks.table.worker;

import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.worker.AsyncJobProgressCallback;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This implementation of CSVWriterStream will notify that progress is made for
 * each row written.
 * 
 * @author John
 * 
 */
public class ProgressingCSVWriterStream implements CSVWriterStream {

	private static final String BUILDING_THE_CSV = "Building the CSV...";
	/**
	 * The number of milliseconds between updates.
	 * 
	 */
	public static final long UPDATE_FEQUENCY_MS = 2000;
	CSVWriter writer;
	AsyncJobProgressCallback progressCallback;
	long currentProgress;
	long totalProgress;
	Clock clock;
	/**
	 * The time of the last progress update.
	 */
	long lastUpdateTimeMS;

	/**
	 * 
	 * @param writer
	 *            Each row will be passed to this writer.
	 * @param progress
	 *            Progress will be reported to this object.
	 * @param originatingMessage
	 *            The original message that started this job. The visibility
	 *            timeout for this message will get extended as long progress
	 *            continues to be made.
	 */
	public ProgressingCSVWriterStream(CSVWriter writer, AsyncJobProgressCallback progressCallback,
			long currentProgress, long totalProgress, Clock clock) {
		super();
		this.writer = writer;
		this.progressCallback = progressCallback;
		this.currentProgress = currentProgress;
		this.totalProgress = totalProgress;
		this.clock = clock;
		this.lastUpdateTimeMS = clock.currentTimeMillis();
	}



	@Override
	public void writeNext(String[] nextLine) {
		// We do not want to spam the listeners, so we only update progress every few seconds.
		if(clock.currentTimeMillis() - lastUpdateTimeMS > UPDATE_FEQUENCY_MS){
			// It is time to update the progress
			// Update the status
			progressCallback.updateProgress(BUILDING_THE_CSV, currentProgress, totalProgress);
			// reset the clock
			this.lastUpdateTimeMS = clock.currentTimeMillis();
		}

		// Write the line
		writer.writeNext(nextLine);
		// some progress was made
		currentProgress++;
	}

}
