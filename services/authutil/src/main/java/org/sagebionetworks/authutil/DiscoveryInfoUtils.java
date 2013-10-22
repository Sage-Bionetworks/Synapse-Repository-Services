package org.sagebionetworks.authutil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.UrlIdentifier;
import org.sagebionetworks.repo.model.auth.DiscoveryInfo;

public class DiscoveryInfoUtils {

	@SuppressWarnings("unchecked")
	public static DiscoveryInfo convertObjectToDTO(DiscoveryInformation discInfo) {
		DiscoveryInfo dto = new DiscoveryInfo();
		dto.setOpenIdEndpoint(discInfo.getOPEndpoint().toString());
		
		Identifier identifier = discInfo.getClaimedIdentifier();
		if (identifier != null) {
			dto.setIdentifier(identifier.getIdentifier());
		}
		
		dto.setDelegate(discInfo.getDelegateIdentifier());
		dto.setVersion(discInfo.getVersion());
		dto.setServiceTypes((Set<String>) discInfo.getTypes());
		return dto;
	}
	
	public static DiscoveryInformation convertDTOToObject(DiscoveryInfo discInfo) throws MalformedURLException {
		URL endpoint = new URL(discInfo.getOpenIdEndpoint());
		try {
			Identifier identifier = null;
			if (discInfo.getIdentifier() != null) {
				identifier = new UrlIdentifier(discInfo.getIdentifier());
			}
			
			DiscoveryInformation obj = new DiscoveryInformation(endpoint, identifier, 
					discInfo.getDelegate(), 
					discInfo.getVersion(), 
					discInfo.getServiceTypes());
			return obj;
		} catch (DiscoveryException e) {
			throw new RuntimeException(e);
		}
	}
}