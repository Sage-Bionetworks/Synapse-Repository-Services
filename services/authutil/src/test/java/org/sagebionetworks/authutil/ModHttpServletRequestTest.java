package org.sagebionetworks.authutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
	void testGetDateHeaderOriginalHeaders() {
		String dateHeaderName = "DATE HEADER";
		long originalDateValue = 1234567L;

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

		verify(originalRequest, times(2)).getDateHeader(dateHeaderName);
	}

	@Test
	void testGetDateHeaderOverrideHeaders() {
		String dateHeaderName = "DATE HEADER";
		long overrideDateValue = 7654321L;

		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);

		// check the case of no such header in the overriding list
		// method under test
		assertEquals(-1L, modifiedRequest.getDateHeader(dateHeaderName));
		// what if we DO set a value?
		headers.put(dateHeaderName, new String[] {""+overrideDateValue, "9999"});
		modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		// method under test
		assertEquals(overrideDateValue, modifiedRequest.getDateHeader(dateHeaderName));

		verify(originalRequest, never()).getDateHeader(dateHeaderName);
	}

	@Test
	void testGetHeaderOriginalHeaders() {
		String headerName = "HEADER";
		String originalValue = "orig";

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

		verify(originalRequest, times(2)).getHeader(headerName);
	}
		
	@Test
	void testGetHeaderOverrideHeaders() {
			String headerName = "HEADER";
			String overrideValue = "override";

		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getHeader(headerName));
		// what if we DO set a value?
		headers.put(headerName, new String[] {overrideValue, "some other value"});
		modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		// method under test
		assertEquals(overrideValue, modifiedRequest.getHeader(headerName));
		
		verify(originalRequest, never()).getHeader(headerName);
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
	void testGetHeadersOriginalHeaders() {
		String headerName = "HEADER";
		String[] originalValues = new String[] {"orig1", "orig2"};

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

		verify(originalRequest, times(2)).getHeaders(headerName);
	}
	
	@Test
	void testGetHeadersOverrideHeaders() {
		String headerName = "HEADER";
		String[] overrideValues = new String[] {"override1", "override2"};

		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertTrue(enumerationsAreEqual(Collections.emptyEnumeration(), modifiedRequest.getHeaders(headerName)));
		// what if we DO set a value?
		headers.put(headerName, overrideValues);
		modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		// method under test
		assertTrue(enumerationsAreEqual(Collections.enumeration(Arrays.asList(overrideValues)), modifiedRequest.getHeaders(headerName)));
		
		verify(originalRequest, never()).getHeader(headerName);
	}

	@Test
	void testGetHeaderNamesOriginalHeaders() {
		String[] originalNames = new String[] {"origName1", "origName2"};

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

		verify(originalRequest, times(2)).getHeaderNames();
	}
	

	@Test
	void testGetHeaderNamesOverrideHeaders() {
		String[] overrideNames = new String[] {"overrideName1", "overrideName2"};

		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		
		// check the case of no headers in the overriding list
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getHeaderNames()));
		// what if we DO set a value?
		for (String headerName : overrideNames) {
			headers.put(headerName, new String[] {});
		}
		modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(overrideNames)), modifiedRequest.getHeaderNames()));

		verify(originalRequest, never()).getHeaderNames();
	}
	
	@Test
	void testGetIntHeaderOriginalHeaders() {
		String headerName = "INT HEADER";
		int originalIntValue = 1234567;

		// check the case that we do not override the headers
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, null);
		when(originalRequest.getIntHeader(headerName)).thenReturn(-1);
		// what if we don't set any value?
		// method under test
		assertEquals(-1, modifiedRequest.getIntHeader(headerName));
		
		// what if we DO set a value?
		when(originalRequest.getIntHeader(headerName)).thenReturn(originalIntValue);
		// method under test
		assertEquals(originalIntValue, modifiedRequest.getIntHeader(headerName));

		verify(originalRequest, times(2)).getIntHeader(headerName);
	}
	
	@Test
	void testGetIntHeaderOverrideHeaders() {
		String headerName = "INT HEADER";
		int overrideIntValue = 7654321;

		// now override the headers
		Map<String,String[]> headers = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		
		// check the case of no such header in the overriding list
		// method under test
		assertEquals(-1L, modifiedRequest.getIntHeader(headerName));
		// what if we DO set a value?
		headers.put(headerName, new String[] {""+overrideIntValue, "9999"});
		modifiedRequest = new ModHttpServletRequest(originalRequest, headers, null);
		// method under test
		assertEquals(overrideIntValue, modifiedRequest.getIntHeader(headerName));

		verify(originalRequest, never()).getIntHeader(headerName);
	}

	@Test
	void testGetParameterOriginalValues() {
		String parameterName = "PARAMETER";
		String originalValue = "orig";

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

		verify(originalRequest, times(2)).getParameter(parameterName);
	}
	
	@Test
	void testGetParameterOverrideValues() {
		String parameterName = "PARAMETER";
		String overrideValue = "override";
		// now override the parameters
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getParameter(parameterName));
		// what if we DO set a value?
		parameters.put(parameterName, new String[] {overrideValue, "some other value"});
		modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		// method under test
		assertEquals(overrideValue, modifiedRequest.getParameter(parameterName));
		
		verify(originalRequest, never()).getParameter(parameterName);
	}
	
	@Test
	void testGetParameterMapOriginalValues() {
		String parameterName = "parameter";

		Map<String, String[]> originalMap = ImmutableMap.of(parameterName, new String[] {"orig1", "orig2"});

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

		verify(originalRequest, times(2)).getParameterMap();
	}
	
	@Test
	void testGetParameterMapOverrideValues() {
		String parameterName = "parameter";
		Map<String, String[]> overrideMap = ImmutableMap.of(parameterName, new String[] {"override1", "override2"});

		// now override the parameters
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, overrideMap);
		
		// method under test
		assertEquals(overrideMap, modifiedRequest.getParameterMap());	

		verify(originalRequest, never()).getParameterMap();
	}
	
	@Test
	void testGetParameterNamesOriginalCValues() {
		String[] originalNames = new String[] {"origName1", "origName2"};

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

		verify(originalRequest, times(2)).getParameterNames();
	}
	
	@Test
	void testGetParameterNamesOverrideValues() {
		String[] overrideNames = new String[] {"overrideName1", "overrideName2"};

		// now override the parameters
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		
		// check the case of no headers in the overriding list
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.emptyEnumeration(), modifiedRequest.getParameterNames()));
		// what if we DO set a value?
		for (String parameterName : overrideNames) {
			parameters.put(parameterName, new String[] {});
		}
		modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		// method under test
		assertTrue(enumerationsHaveSameContent_caseInsensitive(Collections.enumeration(Arrays.asList(overrideNames)), modifiedRequest.getParameterNames()));

		verify(originalRequest, never()).getParameterNames();
	}
	

	@Test
	void testGetParameterValuesOriginalValues() {
		String parameterName = "parameter";
		String[] originalValues = new String[] {"orig1","orig2"};

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

		verify(originalRequest, times(2)).getParameterValues(parameterName);
	}

	@Test
	void testGetParameterValuesOverrideValues() {
		String parameterName = "parameter";
		String[] overrideValues = new String[] {"override1","override2"};
		// now override the headers
		Map<String,String[]> parameters = new HashMap<String,String[]>();
		ModHttpServletRequest modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		
		// check the case of no such header in the overriding list
		// method under test
		assertNull(modifiedRequest.getParameterValues(parameterName));
		// what if we DO set a value?
		parameters.put(parameterName, overrideValues);
		modifiedRequest = new ModHttpServletRequest(originalRequest, null, parameters);
		// method under test
		assertEquals(overrideValues, modifiedRequest.getParameterValues(parameterName));		

		verify(originalRequest, never()).getParameterValues(parameterName);
	}
	
}
