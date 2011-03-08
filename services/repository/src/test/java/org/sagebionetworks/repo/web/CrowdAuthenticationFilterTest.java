package org.sagebionetworks.repo.web;


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

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		filterParams.clear();
		filterParams.put("crowd-protocol", "https");
		filterParams.put("crowd-host", "ec2-50-16-158-220.compute-1.amazonaws.com");
		filterParams.put("crowd-port", "8443");
		filterParams.put("allow-anonymous", "true");
	}
	
	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void test() throws Exception {
//		Assert.fail("Hello world");
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
		f.revalidate("Gr53Xi399cK00ZjvlBxeRg00");
	}

}
