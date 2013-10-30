package org.sagebionetworks.usagemetrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Crowd is no longer in use
 */
@Deprecated
public class CrowdLogParser {
	
	// Note:  To get the log files it's:
	// scp -i PlatformKeyPairEast.pem ec2-user@prod-crowd.sagebase.org:/usr/local/atlassian-crowd-2.3.1/apache-tomcat/logs/localhost_access_log.*.txt <localdir> 
	
	/**
	 * @param 0 - directory of log files
	 * @param 1 - number of days in window
	 * @param 2 (optional) - an end date in format dd-MMM-yyyy (ex: 19-APR-2013). Default is today. 
	 */
	public static void main(String[] args) throws Exception {
		if (args.length<2) throw new IllegalArgumentException("must specify, log directory, number of days in window. End date (dd-MMM-yyyy) is optional (default to today)");
		String dir = args[0];
		if (!(new File(dir)).exists()) throw new IllegalArgumentException(dir+" does not exist.");
		String numDaysString = args[1];
		int numDays = Integer.parseInt(numDaysString);
		String endDateString = args.length<3 ? null : args[2];
		Date endDate = null;
		DateFormat df = new SimpleDateFormat(INPUT_DATE_TIME_FORMAT);
		if (endDateString==null) {
			endDate = new Date(); // now
		} else {
			endDate = df.parse(endDateString);
		}
		
		Date startDate = new Date(endDate.getTime()-numDays*24*60*60*1000L);
		
		Map<String, Collection<Long>> userDays = parseAuthenticationEvents(new File(dir), startDate.getTime(), endDate.getTime());
		System.out.println("Total number of users: "+userDays.size()+" in "+numDays+" day window from "+df.format(startDate)+" to "+df.format(endDate));
		
		// now get the users who logged in >= 3 times, omitting sage employees
		// the following maps email domain to user
		Map<String, Collection<String>> frequentOutsideUsers = new HashMap<String, Collection<String>>();
		for (String name : userDays.keySet()) {
			int at = name.indexOf("@");
			if (at<0) {
				System.out.println("Unexpected user name: "+name);
				continue;
			}
			if (UsageMetricsUtils.isOmittedName(name)) continue;
			Collection<Long> days = userDays.get(name);
			if (days.size() < 3) continue;
			String domain = name.substring(at+1);
			Collection<String> names = frequentOutsideUsers.get(domain);
			if (names==null) {
				names = new HashSet<String>();
				frequentOutsideUsers.put(domain, names);
			}
			names.add(name);
		}
		int totalScore = 0;
		System.out.println("Frequent outside users:");
		for (String domain : frequentOutsideUsers.keySet()) {
			Collection<String> names = frequentOutsideUsers.get(domain);
			System.out.println("\n"+domain+" - "+names.size()+" frequent user(s).");
			for (String name : names) {
				System.out.print(name+" :");
				Collection<Long> days = userDays.get(name);
				Iterator<Long> it = days.iterator();
				for (int i=0; i<3; i++) {
					System.out.print(" "+df.format(it.next()));
				}
				if (it.hasNext()) System.out.print(" ...");
				System.out.println(""); // end line
			}
			// first user in the domain counts double
			totalScore += 1 + frequentOutsideUsers.get(domain).size();
		}
		System.out.println("\nTotal user score: "+totalScore);
	}
	
	
	public static final String INPUT_DATE_TIME_FORMAT = "dd-MMM-yyyy";
	
	public static final String LOG_DATE_TIME_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";
	/**
	 * @param dir  folder containing tom cat log file from Crowd server
	 * @return  authenticating users, binned by *day*
	 * 
	 * A re-authentication event is recognized by a series of two lines:
	 * (1) A request to /session with the session token to be re-authenticated, followed by
	 * (2) A request to /user with a username param, to get the user attributes
	 * 
	 * Example:
	 * 50.16.26.176 - - [30/Apr/2012:04:31:42 +0000] POST /crowd/rest/usermanagement/latest/session/sAKU25skdjhkqvt0g00 HTTP/1.1 200 438 0.939
	 * 50.16.26.176 - - [30/Apr/2012:04:31:42 +0000] GET /crowd/rest/usermanagement/latest/user?expand=attributes&username=auser@sagebase.org HTTP/1.1 200 1862 0.031
	 *
	 * It suffices to recognize the second line.
	 */
	static Map<String, Collection<Long>> parseAuthenticationEvents(File dir, long startTimeStamp, long endTimeStamp) throws IOException, ParseException {
		File[] logFiles = dir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return (!f.isDirectory());
			}
		});
		Map<String, Collection<Long>> ans = new HashMap<String, Collection<Long>>();
		//List<AuthEvent> events = new ArrayList<AuthEvent>();
		for (File f : logFiles) {
			System.out.println("Parsing "+f.getAbsolutePath());
			FileInputStream is = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String s = br.readLine();
			while (s!=null) {
				AuthEvent ae = parseAuthEvent(s);
				if (ae!=null && startTimeStamp<ae.getTimestamp() && ae.getTimestamp()<=endTimeStamp) {
					//events.add(ae);
					Long day = getDayFromTimeStamp(ae.getTimestamp());
					String name = ae.getUserName();
					Collection<Long> days = ans.get(name);
					if (days==null) {
						days = new TreeSet<Long>();
						ans.put(name, days);
					}
					days.add(day);
				}
				s = br.readLine();
			}
			br.close();
			is.close();
		}
		return ans;
	}
	
	
	/**
	 * 
	 * 
	 * @param s one line from a log file
	 * @return the AuthEvent for that line, or null if not an line from an auth event
	 * @throws ParseException
	 */
	public static AuthEvent parseAuthEvent(String s) throws ParseException {
		DateFormat df = new SimpleDateFormat(LOG_DATE_TIME_FORMAT);
		int uriIndex = s.indexOf("/user?");
		int userNameIndex = s.indexOf("&username=");
		//System.out.println("uriIndex: "+uriIndex+" userNameIndex: "+userNameIndex+" for "+s);
		if (uriIndex>0 && userNameIndex>0) {
			// get time stamp
			int timeStart = s.indexOf("[");
			int timeEnd = s.indexOf("]");
			String tsString = s.substring(timeStart+1, timeEnd);
			Date d = df.parse(tsString);
			// get user name
			String userName = s.substring(userNameIndex+"&username".length()+1, s.indexOf(" ", userNameIndex));
			return new AuthEvent(userName, d.getTime());
		}
		return null;
	}
	
	public static long getDayFromTimeStamp(long ts) {
		Calendar timeStamp = new GregorianCalendar();
		timeStamp.setTime(new Date(ts));
		Calendar day = new GregorianCalendar(timeStamp.get(Calendar.YEAR), timeStamp.get(Calendar.MONTH), timeStamp.get(Calendar.DAY_OF_MONTH));
		return day.getTimeInMillis();
	}
	

}
