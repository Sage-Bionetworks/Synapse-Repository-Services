package org.sagebionetworks.logging.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;

import org.joda.time.DateTime;
import org.sagebionetworks.logging.SynapseLoggingUtils;
import org.sagebionetworks.logging.collate.LogEvent;

/**
 * This class depends on the Activity Logs being in a single line-per-event
 * format, with the date the first thing on the line.
 *
 * @author geoff
 *
 */
public class ActivityLogReader implements LogReader {

	public static class ActivityLogReaderFactory implements LogReader.LogReaderFactory<ActivityLogReader> {
		@Override
		public ActivityLogReader create(BufferedReader rdr) {
			return new ActivityLogReader(rdr);
		}
	}

	private BufferedReader reader;

	private ActivityLogReader(BufferedReader rdr) {
		this.reader = rdr;
	}

	@Override
	public LogEvent readLogEvent() throws IOException {
		String line = reader.readLine();
		if (line == null) return null;

		DateTime timestamp = extractTimestamp(line);
		return new LogEvent(timestamp, line);
	}

	private DateTime extractTimestamp(String line) {
		Matcher matcher = SynapseLoggingUtils.DATE_PATTERN.matcher(line);
		if (matcher.find())
			return SynapseLoggingUtils.DATE_FORMATTER.parseDateTime(matcher.group(1));
		else
			throw new IllegalArgumentException("Malformed log line: " + line);
	}

}
