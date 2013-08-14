package org.sagebionetworks.audit.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;

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
	static final String KEY_TEMPLATE = INSTANCE_PREFIX_TEMPLATE+"/%2$04d-%3$02d-%4$02d/%5$02d/%6$s.csv.gz";

	/**
	 * Create a new Key.
	 * @return
	 */
	public static String createNewKey(int stackInstanceNumber, long timeMS){
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(new Date(timeMS));
	    int year = cal.get(Calendar.YEAR);
	    int month = cal.get(Calendar.MONTH);
	    int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
	    return createKey(stackInstanceNumber, year, month, day, hour, UUID.randomUUID().toString());
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
	public static String createKey(int instance, int year, int month, int day, int hour, String uuid){
		return String.format(KEY_TEMPLATE, instance, year, month, day, hour, uuid);
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
