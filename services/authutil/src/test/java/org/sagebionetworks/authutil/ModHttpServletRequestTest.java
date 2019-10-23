package org.sagebionetworks.authutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableMap; 

@ExtendWith(MockitoExtension.class)
public class ModHttpServletRequestTest {
	
	
	@Mock
	private HttpServletRequest originalRequest;
	
	@Test
	void testGetDateHeader() {
		String dateHeaderName = "DATE HEADER";
		long originalDateValue = 1234567L;
		long overrideDateValue = 7654321L;

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getDateHeader(dateHeaderName)).thenReturn(-1L);
		// what if we don't set any value?
		// method under test
		assertEquals(-1L, modifiedRequest.getDateHeader(dateHeaderName));
		
		// what if we DO set a value?
		when(originalRequest.getDateHeader(dateHeaderName)).thenReturn(originalDateValue);
		// method under test
		assertEquals(originalDateValue, modifiedRequest.getDateHeader(dateHeaderName));
		
		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertEquals(-1L, modifiedRequest.getDateHeader(dateHeaderName));
		// what if we DO set a value?
		headers.put(dateHeaderName, new String[] {""+overrideDateValue, "9999"});
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		// method under test
		assertEquals(overrideDateValue, modifiedRequest.getDateHeader(dateHeaderName));
		
	}

	@Test
	void testGetHeader() {
		String headerName = "HEADER";
		String originalValue = "orig";
		String overrideValue = "override";

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getHeader(headerName)).thenReturn(null);
		// what if we don't set any value?
		// method under test
		assertNull(modifiedRequest.getHeader(headerName));
		
		// what if we DO set a value?
		when(originalRequest.getHeader(headerName)).thenReturn(originalValue);
		// method under test
		assertEquals(originalValue, modifiedRequest.getHeader(headerName));
		
		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getHeader(headerName));
		// what if we DO set a value?
		headers.put(headerName, new String[] {overrideValue, "some other value"});
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		// method under test
		assertEquals(overrideValue, modifiedRequest.getHeader(headerName));
		
	}
	
	private static <T> boolean enumerationsAreEqual(Enumeration<T> a, Enumeration<T> b ) {
		while (a.hasMoreElements()) {
			if (!b.hasMoreElements()) return false;
			if (!a.nextElement().equals(b.nextElement())) return false;
		}
		if (b.hasMoreElements()) return false;
		return true;
	}
	
	private static Set<String> enumerationToSetCaseInsensitive(Enumeration<String> e) {
		Set<String> result = new HashSet<String>();
		while (e.hasMoreElements()) {
			result.add(e.nextElement().toLowerCase());
		}
		return result;
	}
	
	private static boolean enumerationsHaveSameContent_caseInsensitive(Enumeration<String> a, Enumeration<String> b ) {
		return enumerationToSetCaseInsensitive(a).equals(enumerationToSetCaseInsensitive(b));
	}

	@Test
	void testGetHeaders() {
		String headerName = "HEADER";
		String[] originalValues = new String[] {"orig1", "orig2"};
		String[] overrideValues = new String[] {"override1", "override2"};

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getHeaders(headerName)).thenReturn(Collections.emptyEnumeration());
		// what if we don't set any value?
		// method under test
		assertFalse(modifiedRequest.getHeaders(headerName).hasMoreElements());
		
		// what if we DO set a value?
		when(originalRequest.getHeaders(headerName)).thenReturn(Collections.enumeration(Arrays.asList(originalValues)));
		// method under test
		assertTrue(enumerationsAreEqual(Collections.enumeration(Arrays.asList(originalValues)), modifiedRequest.getHeaders(headerName)));
		
		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertTrue(enumerationsAreEqual(Collections.emptyEnumeration(), modifiedRequest.getHeaders(headerName)));
		// what if we DO set a value?
		headers.put(headerName, overrideValues);
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		// method under test
		assertTrue(enumerationsAreEqual(Collections.enumeration(Arrays.asList(overrideValues)), modifiedRequest.getHeaders(headerName)));
		
	}

	@Test
	void testGetHeaderNames() {
		String[] originalNames = new String[] {"origName1", "origName2"};
		String[] overrideNames = new String[] {"overrideName1", "overrideName2"};

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
		// what if we don't set any value?
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getHeaderNames()));
		
		// what if we DO set a value?
		when(originalRequest.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(originalNames)));
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(originalNames)), modifiedRequest.getHeaderNames()));
		
		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		
		// check the case of no headers in the overriding list
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getHeaderNames()));
		// what if we DO set a value?
		for (String headerName : overrideNames) {
			headers.put(headerName, new String[] {});
		}
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(overrideNames)), modifiedRequest.getHeaderNames()));
	}
	
	@Test
	void testGetIntHeader() {
		String dateHeaderName = "INT HEADER";
		int originalIntValue = 1234567;
		int overrideIntValue = 7654321;

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getIntHeader(dateHeaderName)).thenReturn(-1);
		// what if we don't set any value?
		// method under test
		assertEquals(-1, modifiedRequest.getIntHeader(dateHeaderName));
		
		// what if we DO set a value?
		when(originalRequest.getIntHeader(dateHeaderName)).thenReturn(originalIntValue);
		// method under test
		assertEquals(originalIntValue, modifiedRequest.getIntHeader(dateHeaderName));
		
		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertEquals(-1L, modifiedRequest.getIntHeader(dateHeaderName));
		// what if we DO set a value?
		headers.put(dateHeaderName, new String[] {""+overrideIntValue, "9999"});
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, headers, null);
		// method under test
		assertEquals(overrideIntValue, modifiedRequest.getIntHeader(dateHeaderName));
		
	}

	@Test
	void testGetParameter() {
		String parameterName = "PARAMETER";
		String originalValue = "orig";
		String overrideValue = "override";

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getParameter(parameterName)).thenReturn(null);
		// what if we don't set any value?
		// method under test
		assertNull(modifiedRequest.getParameter(parameterName));
		
		// what if we DO set a value?
		when(originalRequest.getParameter(parameterName)).thenReturn(originalValue);
		// method under test
		assertEquals(originalValue, modifiedRequest.getParameter(parameterName));
		
		// now override the parameters
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getParameter(parameterName));
		// what if we DO set a value?
		parameters.put(parameterName, new String[] {overrideValue, "some other value"});
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		// method under test
		assertEquals(overrideValue, modifiedRequest.getParameter(parameterName));
		
	}
	
	@Test
	void testGetParameterMap() {
		String parameterName = "parameter";

		Map<String, String[]> originalMap = ImmutableMap.of(parameterName, new String[] {"orig1", "orig2"});
		Map<String, String[]> overrideMap = ImmutableMap.of(parameterName, new String[] {"override1", "override2"});

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getParameterMap()).thenReturn(Collections.EMPTY_MAP);
		// what if we don't set any value?
		// method under test
		assertTrue(modifiedRequest.getParameterMap().isEmpty());
		
		// what if we DO set a value?
		when(originalRequest.getParameterMap()).thenReturn(originalMap);
		// method under test
		assertEquals(originalMap, modifiedRequest.getParameterMap());
		
		// now override the parameters
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, overrideMap);
		
		// method under test
		assertEquals(overrideMap, modifiedRequest.getParameterMap());
		
	}
	
	@Test
	void testGetParameterNames() {
		String[] originalNames = new String[] {"origName1", "origName2"};
		String[] overrideNames = new String[] {"overrideName1", "overrideName2"};

		// check the case that we do not override the parameters
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getParameterNames()).thenReturn(Collections.emptyEnumeration());
		// what if we don't set any value?
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getParameterNames()));
		
		// what if we DO set a value?
		when(originalRequest.getParameterNames()).thenReturn(Collections.enumeration(Arrays.asList(originalNames)));
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(originalNames)), modifiedRequest.getParameterNames()));
		
		// now override the parameters
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		
		// check the case of no headers in the overriding list
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getParameterNames()));
		// what if we DO set a value?
		for (String parameterName : overrideNames) {
			parameters.put(parameterName, new String[] {});
		}
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(overrideNames)), modifiedRequest.getParameterNames()));
	}
	

	@Test
	void testGetParameterValues() {
		String parameterName = "parameter";
		String[] originalValues = new String[] {"orig1","orig2"};
		String[] overrideValues = new String[] {"override1","override2"};

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getParameterValues(parameterName)).thenReturn(null);
		// what if we don't set any value?
		// method under test
		assertNull(modifiedRequest.getParameterValues(parameterName));
		
		// what if we DO set a value?
		when(originalRequest.getParameterValues(parameterName)).thenReturn(originalValues);
		// method under test
		assertEquals(originalValues, modifiedRequest.getParameterValues(parameterName));
		
		// now override the headers
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getParameterValues(parameterName));
		// what if we DO set a value?
		parameters.put(parameterName, overrideValues);
		modifiedRequest = new ModHttpServletRequest(modifiedRequest, null, parameters);
		// method under test
		assertEquals(overrideValues, modifiedRequest.getParameterValues(parameterName));
		
	}
	
}
