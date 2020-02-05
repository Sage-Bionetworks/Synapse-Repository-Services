package org.sagebionetworks.repo.manager.storagelocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BucketOwnerVerifierImpl implements BucketOwnerVerifier {

	public static final String EXTERNAL_STORAGE_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external upload destination.";

	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that %s has permission to write to the bucket. Please create an object in bucket '%s' with key '%s' that contains the text '%s'. Also see "
			+ EXTERNAL_STORAGE_HELP;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	private Map<Class<? extends BucketOwnerStorageLocationSetting>, BucketObjectReader> bucketObjectReaderMap;

	@Autowired
	public void setBucketObjectReader(List<BucketObjectReader> readers) {
		bucketObjectReaderMap = new HashMap<>(readers.size());
		readers.forEach(reader -> {
			bucketObjectReaderMap.put(reader.getSupportedStorageLocationType(), reader);
		});
	}

	@Override
	public void verifyBucketOwnership(UserInfo userInfo, BucketOwnerStorageLocationSetting storageLocation) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(storageLocation, "The storage location");
		ValidateArgument.required(storageLocation.getBucket(), "The bucket");

		BucketObjectReader reader = getObjectReader(storageLocation);

		String bucketName = storageLocation.getBucket();
		String baseKey = storageLocation.getBaseKey();
		String ownerKey = baseKey == null ? OWNER_MARKER : baseKey + "/" + OWNER_MARKER;

		List<PrincipalAlias> ownerAliases = getBucketOwnerAliases(userInfo.getId());

		InputStream stream;

		try {
			stream = reader.openStream(bucketName, ownerKey);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage() + ". " + getExplanation(ownerAliases, bucketName, ownerKey), e);
		}

		BufferedReader content = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

		inspectUsername(content, ownerAliases, bucketName, ownerKey);
	}

	BucketObjectReader getObjectReader(BucketOwnerStorageLocationSetting storageLocation) {
		BucketObjectReader reader = this.bucketObjectReaderMap.get(storageLocation.getClass());
		if (reader == null) {
			throw new IllegalArgumentException("Unsupported storage location type: " + storageLocation.getClass().getSimpleName());
		}
		return reader;
	}

	/**
	 * Collects the possible aliases that can be used to verify ownership of an S3 bucket. Currently, this is a user's
	 * username and their email addresses.
	 * 
	 * @param userId
	 * @return
	 */
	private List<PrincipalAlias> getBucketOwnerAliases(Long userId) {
		return principalAliasDAO.listPrincipalAliases(userId, AliasType.USER_NAME, AliasType.USER_EMAIL);
	}

	void inspectUsername(BufferedReader reader, List<PrincipalAlias> expectedAliases, String bucket, String key) {
		String actualUsername;

		try (BufferedReader br = reader) {
			actualUsername = br.readLine();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read username from key " + key + " from bucket " + bucket + ". "
					+ getExplanation(expectedAliases, bucket, key));
		}

		if (StringUtils.isBlank(actualUsername)) {
			throw new IllegalArgumentException(
					"No username found under key " + key + " from bucket " + bucket + ". " + getExplanation(expectedAliases, bucket, key));
		}

		if (!checkForCorrectName(expectedAliases, actualUsername)) {
			throw new IllegalArgumentException("The username " + actualUsername + " found under key " + key + " from bucket " + bucket
					+ " is not what was expected. " + getExplanation(expectedAliases, bucket, key));
		}
	}

	private static String getExplanation(List<PrincipalAlias> aliases, String bucket, String key) {
		String username = aliases.get(0).getAlias();
		for (PrincipalAlias pa : aliases) {
			if (pa.getType().equals(AliasType.USER_NAME)) {
				username = pa.getAlias();
				break;
			}
		}
		return String.format(SECURITY_EXPLANATION, username, bucket, key, username);
	}

	private static boolean checkForCorrectName(List<PrincipalAlias> allowedNames, String actualUsername) {
		if (allowedNames != null) {
			for (PrincipalAlias name : allowedNames) {
				if (name.getAlias().equalsIgnoreCase(actualUsername)) {
					return true;
				}
			}
		}
		return false;
	}

}
