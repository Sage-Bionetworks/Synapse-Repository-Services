package org.sagebionetworks.logging.collate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sagebionetworks.logging.reader.LogReader;

/**
 * Set of utility functions for collating a set of log files into one.
 * 
 * There are a couple of preconditions for use:
 * 
 *  - the files should all be the same format.  This is meant to be used on a set of
 *    log files generated on different instances of a single Elastic Beanstalk.
 *  - The log files should be from the same period of time.  It will work to use collate on a 
 *    set of log files that have totally non-overlapping timestamp ranges, but it's inefficent.
 *    Those files can simply be concatenated.
 * @author geoff
 *
 */
public class LogCollationUtils {

	public static <T extends LogReader> ArrayList<T> initializeReaders(LogReader.LogReaderFactory<T> factory, List<File> files) throws FileNotFoundException {
		ArrayList<T> readers = new ArrayList<T>();

		for (File file : files) {
			readers.add(factory.create(new BufferedReader(new FileReader(file))));
		}
		return readers;
	}

	public static <T extends LogReader> SortedMap<LogEvent, T> primeCollationMap(List<T> readers) throws IOException {
		SortedMap<LogEvent, T> fileEventMap = new TreeMap<LogEvent, T>();
		for (T stream : readers) {
			LogEvent event = stream.readLogEvent();
			fileEventMap.put(event, stream);
		}
		return fileEventMap;
	}

	public static <T extends LogReader> void collateLogs(SortedMap<LogEvent, T> fileEventMap, BufferedWriter output) throws IOException {

		while (!fileEventMap.isEmpty()) {
			LogEvent firstEvent = fileEventMap.firstKey();
			T rdr = fileEventMap.get(firstEvent);

			output.write(firstEvent.getLine());
			output.newLine();

			fileEventMap.remove(firstEvent);

			LogEvent next = rdr.readLogEvent();
			if (next != null)
				fileEventMap.put(next, rdr);
		}
		output.flush();
	}

}
