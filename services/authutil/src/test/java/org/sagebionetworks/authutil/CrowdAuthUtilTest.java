package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.auth.User;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;

public class CrowdAuthUtilTest {
	private static final String TEST_USER = "demouser@sagebase.org";

	
	@Before
	public void setUp() {
		// this attempts to clean up any users created in a previous run and not deleted
		for (int i=0; i<NUM_USERS; i++) {
			try {
				User user = CrowdAuthUtil.getUser(userID(i));
				CrowdAuthUtil.deleteUser(user.getEmail());
			} catch (Exception e) {
				// here we are doing the best we can, so just go on to the next one
			}
		}
	}
	
	@After
	public void tearDown() throws Exception {
		deleteUsers();
	}


	@Test
	public void testGetFromXML() throws Exception {
		String s = CrowdAuthUtil.getFromXML("/root/name/@attr", new String("<?xml version='1.0' encoding='UTF-8'?><root><name attr='value'/></root>").getBytes());
		assertEquals("value", s);
	}
	
	@Test
	public void testGetMultiFromXML() throws Exception {
		Collection<String> ss = CrowdAuthUtil.getMultiFromXML("/root/name/@attr", new String("<?xml version='1.0' encoding='UTF-8'?><root><name attr='value'/><name attr='value2'/></root>").getBytes());
		assertEquals(Arrays.asList(new String[]{"value","value2"}), ss);
	}
	
	@Test 
	public void testUserAttributes() throws Exception {
		Map<String, Collection<String>> attributes = new HashMap<String, Collection<String>>();
		attributes.put("foo", Arrays.asList(new String[] {"bar"}));
		CrowdAuthUtil.setUserAttributes(TEST_USER, attributes);
		Map<String, Collection<String>> actual = CrowdAuthUtil.getUserAttributes(TEST_USER);
		assertEquals(attributes.get("foo"), actual.get("foo"));
		CrowdAuthUtil.deleteUserAttribute(TEST_USER, "foo");
		actual = CrowdAuthUtil.getUserAttributes(TEST_USER);
		assertNull(actual.get("foo"));
	}


	
	class MutableBoolean {
		boolean b = false;
		public void set(boolean b) {this.b=b;}
		public boolean get() {return b;}
	}
	
	class MutableLong {
		long L = 0L;
		public void set(long L) {this.L=L;}
		public long get() {return L;}
	}
	
	private static final int NUM_USERS = 1;
	
	public static String userID(int n) {
		if (n<0 || n>=NUM_USERS) throw new IllegalArgumentException();
		return "plfm292_"+n+"@sagebase.org";
	}
	
	public void createUsers() throws Exception {
		long start = System.currentTimeMillis();
		for (int i=0; i<NUM_USERS; i++) {
			User user = new User();
			user.setEmail(userID(i));
			user.setPassword("pw");
			user.setFirstName("foo");
			user.setLastName("bar");
			user.setDisplayName("foobar");
			CrowdAuthUtil.createUser(user);
			usersToDelete.add(user.getEmail());
		}
		System.out.println("Took "+((System.currentTimeMillis()-start)/1000L)+
				" sec to create "+NUM_USERS+" users.");
	}
	
	
	public void deleteUsers() throws Exception {
		long start = System.currentTimeMillis();
		for (String s : usersToDelete) {
			User user = new User();
			user.setEmail(s);
			CrowdAuthUtil.deleteUser(user.getEmail());
		}
		if (usersToDelete.size()>0) { 
			System.out.println("Took "+((System.currentTimeMillis()-start)/1000L)+
				" sec to delete "+usersToDelete.size()+" users.");
		}
		usersToDelete.clear();
	}
	
	private List<String> usersToDelete = new ArrayList<String>();
	
	// this is meant to recreate the problem described in PLFM-292
	// http://sagebionetworks.jira.com/browse/PLFM-292
	@Test 
	public void testMultipleLogins() throws Exception {
		HttpClientHelper.setGlobalConnectionTimeout(DefaultHttpClientSingleton.getInstance(), 100000);
		HttpClientHelper.setGlobalSocketTimeout(DefaultHttpClientSingleton.getInstance(), 100000);
		createUsers();
		for (int i : new int[]{35}) {
//		for (int i : new int[]{1,2,3,4,5,6}) {
			testMultipleLogins(i);
		}
	}
	
	private static final long FAILURE_VALUE = -1L;
		
	public void testMultipleLogins(int n) throws Exception {
//		long start = System.currentTimeMillis();
		Map<Integer, MutableLong> times = new HashMap<Integer, MutableLong>();
		for (int i=0; i<n; i++) {
			final int fi = i;
			final MutableLong L = new MutableLong();
			times.put(i, L);
		 	Thread thread = new Thread() {
				public void run() {
					try {
						User user = new User();
						user.setEmail(userID(fi % NUM_USERS));
						long start = System.currentTimeMillis();
						CrowdAuthUtil.authenticate(user, false);
						L.set(System.currentTimeMillis()-start);
					} catch (Exception e) {
						L.set(FAILURE_VALUE);
						fail(e.toString());
					}
				}
			};
			thread.start();
		}
		int count = 0;
		long elapsed = 0L;
		Set<Long> sortedTimes = new TreeSet<Long>();
		while (!times.isEmpty()) {
			for (int i: times.keySet()) {
				long L = times.get(i).get();
				if (L!=0) {
					if (L==FAILURE_VALUE) {
						// don't add to stat's
					} else {
						elapsed += L;
						//System.out.println((float)L/1000L+" sec.");
						sortedTimes.add(L);
						count++;
					}
					times.remove(i);
					break;
				}
			}
		}
		System.out.println(count+" authentication request response time (sec): min "+
				((float)sortedTimes.iterator().next()/1000L)+" avg "+((float)elapsed/count/1000L)+
				" max "+((float)getLast(sortedTimes)/1000L));
	}

	private static <T> T getLast(Set<T> set) {
		T ans = null;
		for (T v : set) ans=v;
		return ans;
	}

}
