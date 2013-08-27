package org.sagebionetworks.audit.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Utility for generating key for AccessRecords batches.
 * 
 * @author John
 *
 */
public class KeyGeneratorUtil {
		
	/**
	 * This template is used to generated a key for a batch of AccessRecords:
	 * <stack_instance>/<year><month><day>/<hour>/<uuid>.csv.gz
	 */
	static final String INSTANCE_PREFIX_TEMPLATE = "%1$09d";
	static final String DATE_TEMPLATE = "%1$04d-%2$02d-%3$02d";
	static final String KEY_TEMPLATE = "%1$S/%2$S/%3$02d-%4$02d-%5$02d-%6$03d-%7$s.csv.gz";

	/**
	 * Create a new Key.
	 * @return
	 */
	public static String createNewKey(int stackInstanceNumber, long timeMS){
	    Calendar cal = getCalendarUTC(timeMS);
	    int year = cal.get(Calendar.YEAR);
	    // We do a +1 because JANUARY=0 
	    int month = cal.get(Calendar.MONTH) +1;
	    int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int mins = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int milli = cal.get(Calendar.MILLISECOND);
	    return createKey(stackInstanceNumber, year, month, day, hour, mins, sec, milli, UUID.randomUUID().toString());
	}


	private static Calendar getCalendarUTC(long timeMS) {
		Calendar cal = Calendar.getInstance();
	    cal.setTime(new Date(timeMS));
		return cal;
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
	public static String createKey(int instance, int year, int month, int day, int hour, int min, int sec, int milli, String uuid){
		return String.format(KEY_TEMPLATE, getInstancePrefix(instance), getDateString(year, month, day), hour, min, sec, milli, uuid);
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
	 * Extract the date string from a key
	 * @param key
	 * @return
	 */
	public static String getDateStringFromKey(String key){
		String[] split = key.split("/");
		return split[1];
	}
	
	/**
	 * Extract the date and hour from the key
	 * @param key
	 * @return
	 */
	public static String getDateAndHourFromKey(String key){
		String[] split = key.split("/");
		StringBuilder builder = new StringBuilder();
		builder.append(split[1]);
		builder.append("/");
		builder.append(split[2].substring(0, 2));
		return builder.toString();
	}
}
