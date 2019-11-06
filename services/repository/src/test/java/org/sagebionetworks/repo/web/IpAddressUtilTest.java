package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressUtilTest {
	
	@Mock
	HttpServletRequest request;
	
	/////////////////////
	//getIpAddress Tests
	/////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetIpAddressNullRequest() {
		IpAddressUtil.getIpAddress(null);
	}
	
	@Test
	public void testGetIpAddressNoXForwardedFor(){
		String ip = "123.123.123.123";
		when(request.getHeader(IpAddressUtil.X_FORWARDED_FOR)).thenReturn(null);
		when(request.getRemoteAddr()).thenReturn(ip);
		assertEquals(ip, IpAddressUtil.getIpAddress(request));
	}
	
	@Test
	public void testGetIpAddressHasXForwardedFor(){
		String xForwardedFor = "222.222.222.222, 123.123.123.123";
		when(request.getHeader(IpAddressUtil.X_FORWARDED_FOR)).thenReturn(xForwardedFor);
		when(request.getRemoteAddr()).thenReturn("should not be returned");
		assertEquals(xForwardedFor, IpAddressUtil.getIpAddress(request));
	}
}
