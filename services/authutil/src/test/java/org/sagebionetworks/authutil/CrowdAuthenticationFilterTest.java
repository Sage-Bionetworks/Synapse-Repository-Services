package org.sagebionetworks.authutil;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class CrowdAuthenticationFilterTest {
	private final Map<String,String> filterParams = new HashMap<String, String>();
	
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();


	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		filterParams.clear();
		filterParams.put("allow-anonymous", "true");
	}
	
	/**
	 * @throws Exception
	 */

	@Test
	public void test() throws Exception {

		CrowdAuthenticationFilter f = new CrowdAuthenticationFilter();
		f.init(new FilterConfig() {
			public String getFilterName() {return "";}
			public String getInitParameter(java.lang.String name) {return filterParams.get(name);}
			public Enumeration<String> getInitParameterNames() {
				Set<String> keys = filterParams.keySet();
				final Iterator<String> i = keys.iterator();
				return new Enumeration<String>() {
					public boolean hasMoreElements() {return i.hasNext();}
					public String nextElement() {return i.next();}
				};
			}
			public ServletContext getServletContext() {return null;}
		});
	}
	
	// this is a performance test, not necessary to run in the regression test suite
	@Ignore
	@Test
	public void testMultiRevalidations() throws Exception {
		User creds = new User();
		creds.setEmail("demouser@sagebase.org");
		Session session = crowdAuthUtil.authenticate(creds, false);
		int n = 100;
		long start = System.currentTimeMillis();
		String userId="";
		for (int i=0; i<n; i++) {
			userId = crowdAuthUtil.revalidate(session.getSessionToken());
		}
		System.out.println("userId="+userId+" time for "+n+" revalidation events="+
				(System.currentTimeMillis()-start)/1000L +" seconds.");
	}

}
