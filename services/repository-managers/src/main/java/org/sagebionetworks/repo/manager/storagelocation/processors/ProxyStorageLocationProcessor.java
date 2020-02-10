package org.sagebionetworks.repo.manager.storagelocation.processors;

import java.net.MalformedURLException;
import java.net.URL;

import org.sagebionetworks.repo.manager.storagelocation.StorageLocationProcessor;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class ProxyStorageLocationProcessor implements StorageLocationProcessor<ProxyStorageLocationSettings> {

	public static final int MIN_SECRET_KEY_CHARS = 36;
	
	@Override
	public boolean supports(Class<? extends StorageLocationSetting> storageLocationClass) {
		return ProxyStorageLocationSettings.class.isAssignableFrom(storageLocationClass);
	}
	
	@Override
	public void beforeCreate(UserInfo userInfo, ProxyStorageLocationSettings storageLocation) {
		ValidateArgument.required(storageLocation.getProxyUrl(), "The proxyUrl");
		ValidateArgument.required(storageLocation.getSecretKey(), "The secretKey");
		if (storageLocation.getSecretKey().length() < MIN_SECRET_KEY_CHARS) {
			throw new IllegalArgumentException("SecretKey must be at least: " + MIN_SECRET_KEY_CHARS
					+ " characters but was: " + storageLocation.getSecretKey().length());
		}
		try {
			URL proxyUrl = new URL(storageLocation.getProxyUrl());
			if (!"https".equals(proxyUrl.getProtocol())) {
				throw new IllegalArgumentException("The proxyUrl protocol must be be HTTPS");
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("The proxyUrl is malformed: " + e.getMessage());
		}
	}
}
