package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class IpAddressUtilTest {
	
	@Mock
	private HttpServletRequest request;
	
	/////////////////////
	//getIpAddress Tests
	/////////////////////
	
	@Test
	public void testGetIpAddressNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {			
			IpAddressUtil.getIpAddress(null);
		});
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
		assertEquals(xForwardedFor, IpAddressUtil.getIpAddress(request));
		verify(request, never()).getRemoteAddr();
	}
}
