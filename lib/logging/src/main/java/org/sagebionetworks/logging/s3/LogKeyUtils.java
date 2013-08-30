package org.sagebionetworks.logging.s3;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class LogKeyUtils {

	static final String INSTANCE_PREFIX_TEMPLATE = "%1$09d";
	static final String DATE_TEMPLATE = "%1$04d-%2$02d-%3$02d";
	static final String KEY_TEMPLATE = "%1$s/%2$s/%3$s/%4$02d-%5$02d-%6$02d-%7$03d-%8$s.log.gz";
	
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
		Calendar cal = getCalendarUTC(lastModified);
	    int year = cal.get(Calendar.YEAR);
	    // We do a +1 because JANUARY=0 
	    int month = cal.get(Calendar.MONTH) +1;
	    int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int mins = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int milli = cal.get(Calendar.MILLISECOND);
		return createKey(instanceNumber, split[0], year, month, day, hour, mins, sec, milli, UUID.randomUUID().toString());
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
	public static String createKey(int instance, String type, int year, int month, int day, int hour, int min, int sec, int milli, String uuid){
		return String.format(KEY_TEMPLATE, getInstancePrefix(instance), type, getDateString(year, month, day), hour, min, sec, milli, uuid);
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
	    Calendar cal = getCalendarUTC(timeMS);
	    int year = cal.get(Calendar.YEAR);
	    // We do a +1 because JANUARY=0 
	    int month = cal.get(Calendar.MONTH) + 1;
	    int day = cal.get(Calendar.DAY_OF_MONTH);
		return getDateString(year, month, day);
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
	

}
