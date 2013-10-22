package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;
import org.openid4java.discovery.DiscoveryInformation;
import org.sagebionetworks.repo.model.auth.DiscoveryInfo;

public class DiscoveryInfoUtilsTest {
	
	/**
	 * @return A DTO containing some bogus OpenID discovery information
	 */
	public static DiscoveryInfo getTestingDTO() {
		// The strings used in this test are based on examples found online
		// See: http://en.wikipedia.org/wiki/Yadis
		DiscoveryInfo dto = new DiscoveryInfo();
		dto.setOpenIdEndpoint("http://www.foobar.com/server");
		dto.setIdentifier("http://www.foobar.com/users/" + UUID.randomUUID() + "~WHEEEE");
		dto.setDelegate("http://www.foobar.com/users/WOOOOOO/");
		dto.setVersion("version: 1.0");
		dto.setServiceTypes(new HashSet<String>());
		dto.getServiceTypes().add("http://openid.net/signon/1.0");
		return dto;
	}
	
	@Test 
	public void testConversionRoundTrip() throws Exception {
		DiscoveryInfo dto = getTestingDTO();
		
		DiscoveryInformation obj = DiscoveryInfoUtils.convertDTOToObject(dto);
		DiscoveryInfo dto2 = DiscoveryInfoUtils.convertObjectToDTO(obj);
		
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testZipRoundTrip() throws Exception {
		DiscoveryInfo dto = getTestingDTO();
		
		String zipped = DiscoveryInfoUtils.zipDTO(dto);
		DiscoveryInfo dto2 = DiscoveryInfoUtils.unzipDTO(zipped);
		
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testConvertZipRoundTrip() throws Exception {
		// Usually, you start with a DiscoveryInformation object,
		// but that constructor is more troublesome to use
		DiscoveryInfo originalDTO = getTestingDTO();
		DiscoveryInformation obj = DiscoveryInfoUtils.convertDTOToObject(originalDTO);
		
		DiscoveryInfo converted = DiscoveryInfoUtils.convertObjectToDTO(obj);
		String zipped = DiscoveryInfoUtils.zipDTO(converted);
		DiscoveryInfo unzipped = DiscoveryInfoUtils.unzipDTO(zipped);
		
		assertEquals(originalDTO, unzipped);
	}
}
