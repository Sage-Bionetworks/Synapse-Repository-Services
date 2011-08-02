package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CrowdAuthUtilTest {
	
	@Before
	public void setUp() {
		CrowdAuthUtil.acceptAllCertificates();
	}
	
	@After
	public void tearDown() throws Exception {
		// deleteUsers(); temporarily needed for 'testMultipleLogins()'
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
	
	class MutableBoolean {
		boolean b = false;
		public void set(boolean b) {this.b=b;}
		public boolean get() {return b;}
	}
	
	private static final int NUM_USERS = 1;
	
	public static String userID(int n) {
		if (n<0 || n>=NUM_USERS) throw new IllegalArgumentException();
		return "plfm292_"+n+"@sagebase.org";
	}
	
	public void createUsers() throws Exception {
		CrowdAuthUtil cau = new CrowdAuthUtil();
		long start = System.currentTimeMillis();
		for (int i=0; i<NUM_USERS; i++) {
			User user = new User();
			user.setEmail(userID(i));
			user.setPassword("pw");
			user.setFirstName("foo");
			user.setLastName("bar");
			user.setDisplayName("foobar");
			cau.createUser(user);
			usersToDelete.add(user.getEmail());
		}
		System.out.println("Took "+((System.currentTimeMillis()-start)/1000L)+
				" sec to create "+NUM_USERS+" users.");
	}
	
	
	public void deleteUsers() throws Exception {
		final CrowdAuthUtil cau = new CrowdAuthUtil();
		long start = System.currentTimeMillis();
		for (String s : usersToDelete) {
			User user = new User();
			user.setEmail(s);
			cau.deleteUser(user);
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
	@Ignore
	@Test 
	public void testMultipleLogins() throws Exception {
		long start = System.currentTimeMillis();
		final CrowdAuthUtil cau = new CrowdAuthUtil();
		createUsers();
		int n = 250;
		for (int i=0; i<n; i++) {
			final int fi = i;
			final MutableBoolean b = new MutableBoolean();
		 	Thread thread = new Thread() {
				public void run() {
					try {
						User user = new User();
						user.setEmail(userID(fi % NUM_USERS));
						cau.authenticate(user, false);
						//cau.getUser(user.getEmail());
						b.set(true);
					} catch (Exception e) {
						fail(e.toString());
					}
				}
			};
			thread.start();
			long start2 = System.currentTimeMillis();
			try {
//				thread.join(5000L); // time out after 5 sec
				thread.join(30000L); // time out
			} catch (InterruptedException ie) {
				// as expected
			}
			System.out.println(""+i+": done after "+(System.currentTimeMillis()-start2)+" ms.");
//			Thread.sleep(2000L);
			assertTrue("Failed or timed out after "+i+" iterations.", b.get()); // should have been set to 'true' if successful
		}
		System.out.println("Took "+((System.currentTimeMillis()-start)/1000L)+
				" sec to authenticate "+n+" times.");

	}


}
