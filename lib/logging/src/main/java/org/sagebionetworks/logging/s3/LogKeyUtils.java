package org.sagebionetworks.logging.s3;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class LogKeyUtils {

	private static final int ISO08601_MIN_LENGTH = 23;
	static final String INSTANCE_PREFIX_TEMPLATE = "%1$09d";
	static final String DATE_TEMPLATE = "%1$04d-%2$02d-%3$02d";
	static final String KEY_TEMPLATE = "%1$s/%2$s/%3$s/%4$02d-%5$02d-%6$02d-%7$03d-%8$s.log.gz";
	/**
	 * This is the date format we write to the logs: 2013-08-31 17:06:42,368
	 */
	static final String ISO8601_TEMPLATE = "%1$04d-%2$02d-%3$02d %4$02d:%5$02d:%6$02d,%7$03d";
	
	/**
	 * Extract the parts from a timestamp object
	 * @param timestamp
	 * @return
	 */
	public static int[] extractDataParts(long timestamp){
		Calendar cal = getCalendarUTC(timestamp);
		int[] parts = new int[7];
		parts[0] = cal.get(Calendar.YEAR);
	    // We do a +1 because JANUARY=0 
		parts[1] = cal.get(Calendar.MONTH) +1;
		parts[2] = cal.get(Calendar.DAY_OF_MONTH);
		parts[3] = cal.get(Calendar.HOUR_OF_DAY);
		parts[4] = cal.get(Calendar.MINUTE);
		parts[5] = cal.get(Calendar.SECOND);
		parts[6] = cal.get(Calendar.MILLISECOND);
		return parts;
	}
	/**
	 * 
	 * @param instanceNumber
	 * @param fileName
	 * @return
	 */
	public static String createKeyForFile(int instanceNumber, String fileName, long lastModified){
		String[] split = fileName.toLowerCase().split("\\.");
		if(split.length != 4){
			throw new IllegalArgumentException("Unknown FileName: "+fileName+" expected: '<type>.<date>.log.gz'");
		}
		int[] parts = extractDataParts(lastModified);
		return createKey(instanceNumber, split[0], parts, UUID.randomUUID().toString());
	}
	
	/**
	 * Create a key from all of the parts.
	 * @param instance The stack instance number must be padded with 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param uuid
	 * @return
	 */
	public static String createKey(int instance, String type, int[] parts, String uuid){
		return String.format(KEY_TEMPLATE, getInstancePrefix(instance), type, getDateString(parts[0], parts[1], parts[2]), parts[3], parts[4], parts[5], parts[6], uuid);
	}
	/**
	 * Get a new UTC calendar set to the given time.
	 * @param timeMS
	 * @return
	 */
	public static Calendar getCalendarUTC(long timeMS) {
		Calendar cal = getClaendarUTC();
	    cal.setTime(new Date(timeMS));
		return cal;
	}
	
	/**
	 * Get a new Calendar Set to UTC time zone.
	 * @return
	 */
	public static Calendar getClaendarUTC(){
		return Calendar.getInstance(TimeZone.getTimeZone("GMT+0:00"));
	}
	
	/**
	 * Get the date string
	 * @param timeMS
	 * @return
	 */
	public static String getDateString(long timeMS){
		int[] parts = extractDataParts(timeMS);
		return getDateString(parts[0], parts[1], parts[2]);
	}
	
	/**
	 * Get the date String
	 * @param year
	 * @param month
	 * @param day
	 * @return
	 */
	public static String getDateString(int year, int month, int day){
		return String.format(DATE_TEMPLATE, year, month, day);
	}
	
	/**
	 * Get the prefix used for this instance.
	 * @param instance
	 * @return
	 */
	public static String getInstancePrefix(int instance){
		return String.format(INSTANCE_PREFIX_TEMPLATE, instance);
	}
	

	

	/**
	 * Create an ISO8601-GMT date string like what is written to the logs: 2013-08-31 17:06:42,368
	 * @param timestamp
	 * @return
	 */
	public static String createISO8601GMTLogString(long timestamp){
		int[] parts = extractDataParts(timestamp);
		return String.format(ISO8601_TEMPLATE, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5],parts[6]);
	}
	/**
	 * Read the time stamp in MS from a string that starts with ISO8601GMT string.
	 * @param input
	 * @return
	 * @throws ParseException When the passed string does not start with a valid ISO8601GMT string
	 */
	public static long readISO8601GMTFromString(String input) throws ParseException {
		if(input == null) throw new ParseException("Input string is null", 0);
		if(input.length() < ISO08601_MIN_LENGTH) throw new ParseException("Input does not start with a ISO8601 as it is less than "+ISO08601_MIN_LENGTH+" characters", ISO08601_MIN_LENGTH);
		String prefix = input.substring(0, ISO08601_MIN_LENGTH);
		Calendar cal = getClaendarUTC();
		try {
			cal.set(Calendar.YEAR, Integer.parseInt(prefix.substring(0, 4)));
			cal.set(Calendar.MONTH, Integer.parseInt(prefix.substring(5, 7))-1);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(prefix.substring(8, 10)));
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(prefix.substring(11, 13)));
			cal.set(Calendar.MINUTE, Integer.parseInt(prefix.substring(14, 16)));
			cal.set(Calendar.SECOND, Integer.parseInt(prefix.substring(17, 19)));
			cal.set(Calendar.MILLISECOND, Integer.parseInt(prefix.substring(20, 23)));
			return cal.getTimeInMillis();
		} catch (NumberFormatException e) {
			throw new ParseException("Not a ISO8601 string: "+e.getMessage(), 0);
		}
	}
}
