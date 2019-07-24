package org.sagebionetworks.repo.model.dbo.dao;

import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.util.Md5Utils;

public class StorageLocationUtils {

	public static String computeHash(final StorageLocationSetting setting) {
		ValidateArgument.required(setting, "setting");
		StorageLocationSetting normalizedSetting = copyNormalized(setting);
		try {
			byte[] jsonBytes = EntityFactory.createJSONStringForEntity(normalizedSetting).getBytes("UTF-8");
			byte[] md5Bytes = Md5Utils.computeMD5Hash(jsonBytes);
			return new String(Hex.encodeHex(md5Bytes));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}
	
	private static StorageLocationSetting copyNormalized(final StorageLocationSetting setting) {
		try {
			String json = EntityFactory.createJSONStringForEntity(setting);
			StorageLocationSetting normalizedSetting = EntityFactory.createEntityFromJSONString(json, StorageLocationSetting.class);
			normalizedSetting.setStorageLocationId(null);
			normalizedSetting.setEtag(null);
			normalizedSetting.setCreatedOn(null);
			return normalizedSetting;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	
}
