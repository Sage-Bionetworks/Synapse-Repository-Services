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
import org.sagebionetworks.authutil.CrowdAuthenticationFilter;

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

}
