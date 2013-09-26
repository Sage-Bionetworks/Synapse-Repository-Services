package org.sagebionetworks.auth;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * These tests will require the UserManager to be pointed towards RDS rather than Crowd
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthenticationFilterTest {
	
	private AuthenticationFilter filter;

	@Before
	public void setUp() throws Exception {
		final Map<String,String> filterParams = new HashMap<String, String>();
		filterParams.put("allow-anonymous", "true");

		filter = new AuthenticationFilter();
		filter.init(new FilterConfig() {
			public String getFilterName() { 
				return ""; 
			}
			
			public String getInitParameter(String name) {
				return filterParams.get(name);
			}
			
			public Enumeration<String> getInitParameterNames() {
				Set<String> keys = filterParams.keySet();
				final Iterator<String> i = keys.iterator();
				return new Enumeration<String>() {
					public boolean hasMoreElements() {
						return i.hasNext();
					}
					public String nextElement() {
						return i.next();
					}
				};
			}
			
			public ServletContext getServletContext() {
				return null;
			}
		});
	}
	
	@Test
	public void testAnonymous() throws Exception {
		throw new NotImplementedException();
	}
	
	@Test
	public void testSessionToken() throws Exception {
		throw new NotImplementedException();
	}
	
	@Test
	public void testHmac() throws Exception {
		throw new NotImplementedException();
	}

}
