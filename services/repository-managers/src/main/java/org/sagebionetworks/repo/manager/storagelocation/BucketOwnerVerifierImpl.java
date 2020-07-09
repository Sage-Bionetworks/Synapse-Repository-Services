package org.sagebionetworks.repo.manager.storagelocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class BucketOwnerVerifierImpl implements BucketOwnerVerifier {
	
	public static final String EXTERNAL_STORAGE_HELP = "http://docs.synapse.org/articles/custom_storage_location.html for more information on how to create a new external upload destination.";

	private static final String SECURITY_EXPLANATION = "For security purposes, Synapse needs to establish that the user has permission to write to the bucket. Please create an object in bucket '%s' with key '%s' that contains a "
			+ "line separated list of identifiers for the user. Valid identifiers are the id of the user or id of a team the user is part of. Also see "
			+ EXTERNAL_STORAGE_HELP;
	
	// Set of teams that are not allowed to be used as identifiers in the owner.txt
	private static final Set<Long> EXCLUDED_TEAMS = ImmutableSet.of(
		BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
		BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
		BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId()
	);
	
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
		
		Set<String> ownerAliases = getBucketOwnerAliases(userInfo);
		List<String> ownerContent = readOwnerContent(reader, bucketName, ownerKey);
		validateOwnerContent(ownerContent, ownerAliases, bucketName, ownerKey);

	}

	BucketObjectReader getObjectReader(BucketOwnerStorageLocationSetting storageLocation) {
		BucketObjectReader reader = this.bucketObjectReaderMap.get(storageLocation.getClass());
		if (reader == null) {
			throw new IllegalArgumentException("Unsupported storage location type: " + storageLocation.getClass().getSimpleName());
		}
		return reader;
	}
	
	List<String> readOwnerContent(BucketObjectReader reader, String bucketName, String ownerKey) {
		try (InputStream stream = reader.openStream(bucketName, ownerKey)) {

			BufferedReader content = newStreamReader(stream);
			
			return content
					.lines()
					.limit(OWNER_TXT_MAX_LINES)
					// Each line can be a comma separated list of aliases
					.flatMap((line) -> Stream.of(line.split(SAME_LINE_SEPARATOR)))
					.map(this::normalizeAlias)
					.filter( value -> !value.isEmpty())
					.collect(Collectors.toList());
			
		} catch (UncheckedIOException e) {
			throw new IllegalArgumentException("Could not read key " + ownerKey + " from bucket " + bucketName + ". "
					+ getExplanation(bucketName, ownerKey), e);
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage() + ". " + getExplanation(bucketName, ownerKey), e);
		}
	}
	
	BufferedReader newStreamReader(InputStream stream) {
		return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
	}

	void validateOwnerContent(List<String> ownerContent, Set<String> validAliases, String bucket, String key) {
		if (ownerContent.isEmpty()) {
			throw new IllegalArgumentException("No user identifier found under key " + key + " from bucket " + bucket + ". " + getExplanation(bucket, key));
		}

		for (String line : ownerContent) {
			if (validAliases.contains(line)) {
				return;
			}
		}
		
		throw new IllegalArgumentException("Could not find a valid user identifier under key " + key + " from bucket " + bucket
				+ ". " + getExplanation(bucket, key));
		
	}
	
	/**
	 * Collects the possible aliases that can be used to verify ownership of an S3 bucket.
	 * 
	 * @param userInfo
	 * @return
	 */
	Set<String> getBucketOwnerAliases(UserInfo userInfo) {
		Set<String> ownerAliases = new HashSet<>();
		
		// The user id is a valid alias
		ownerAliases.add(userInfo.getId().toString());
		
		List<PrincipalAlias> principalAliases = principalAliasDAO.listPrincipalAliases(userInfo.getId(), AliasType.USER_NAME, AliasType.USER_EMAIL);
		
		// Adds the username and user emails as valid aliases
		ownerAliases.addAll(principalAliases.stream()
				.map(PrincipalAlias::getAlias)
				.map(this::normalizeAlias)
				.collect(Collectors.toList())
		);
		
		// Adds the id of the teams of the user, excluding the certain boostrapped teams (e.g. public group, certified users etc)
		ownerAliases.addAll(userInfo.getGroups().stream()
				.filter( teamId -> !EXCLUDED_TEAMS.contains(teamId))
				.map(Object::toString)
				.collect(Collectors.toList())
		);
		
		return ownerAliases;
	}
	
	private String normalizeAlias(String inputAlias) {
		return inputAlias.trim().toLowerCase();
	}

	private static String getExplanation(String bucket, String key) {
		return String.format(SECURITY_EXPLANATION, bucket, key);
	}

}
