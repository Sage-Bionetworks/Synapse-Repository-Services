package org.sagebionetworks.repo.manager.principal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;


/**
 * Utilities for working with UserProfiles and PrincipalAliases.
 * 
 * @author John
 *
 */
public class UserProfileUtillity {
	
	private static final String TEMPORARY_USERNAME_PREFIX = "TEMPORARY-";

	/**
	 * Merge a UserProfile with its aliases.
	 * @param profile
	 * @param aliases
	 */
	public static void mergeProfileWithAliases(UserProfile profile, List<PrincipalAlias> aliases){
		if(profile == null) throw new IllegalArgumentException("UserProfile cannot be null");
		if(profile.getOwnerId() == null) throw new IllegalArgumentException("UserProfile.ownerID cannot be null");
		if(aliases == null) throw new IllegalArgumentException("Aliases cannot be null");
		// Build up lists of emails and OpenIds found in the bound aliases.
		List<String> newEmails = new LinkedList<String>();
		List<String> newOpenIds = new LinkedList<String>();
		// A single email address is deprecated.
		profile.setEmail(null);
		// This will be re-set below.
		profile.setUserName(null);
		// Set each alias
		for(PrincipalAlias alias: aliases){
			if(AliasType.USER_NAME.equals(alias.getType())){
				// Always set the username 
				profile.setUserName(alias.getAlias());
			}else if(AliasType.USER_EMAIL.equals(alias.getType())){
				newEmails.add(alias.getAlias());
			}else if(AliasType.USER_OPEN_ID.equals(alias.getType())){
				newOpenIds.add(alias.getAlias());
			}
		}
		// If the user name is not set then use a temporary value.
		if(profile.getUserName() == null){
			profile.setUserName(createTempoaryUserName(Long.parseLong(profile.getOwnerId())));
		}
		// If the user already has an email list or an openId list then we want to keep what they have.
		if(isNullOrEmpty(profile.getEmails())){
			// Use the new email list
			profile.setEmails(newEmails);
		}
		if(isNullOrEmpty(profile.getOpenIds())){
			// Use the new email list
			profile.setOpenIds(newOpenIds);
		}
	}
	
	/**
	 * Is the list null or empty
	 * @param list
	 * @return
	 */
	public static boolean isNullOrEmpty(List<String> list){
		if(list == null) return true;
		return list.size() < 1;
	}
	
	/**
	 * Merge a list of UserProfiles with all of the aliases
	 * @param profiles
	 * @param aliases
	 */
	public static void mergeProfileWithAliases(List<UserProfile> profiles, Map<Long, List<PrincipalAlias>> aliases){
		if(profiles == null) throw new IllegalArgumentException("UserProfiles cannot be null");
		if(aliases == null) throw new IllegalArgumentException("Aliases cannot be null");
		for(UserProfile profile: profiles){
			if(profile.getOwnerId() == null) throw new IllegalArgumentException("UserProfile.ownerID cannot be null");
			Long princpalId = Long.parseLong(profile.getOwnerId());
			List<PrincipalAlias> userAliaes = aliases.get(princpalId);
			if(userAliaes != null){
				mergeProfileWithAliases(profile, userAliaes);
			}
		}
	}

	/**
	 * Given a list of aliases from multiple principals, group them by principals.
	 * 
	 * @param aliases
	 * @return
	 */
	public static Map<Long, List<PrincipalAlias>> groupAlieaseByPrincipal(List<PrincipalAlias> aliases){
		if(aliases == null) throw new IllegalArgumentException("Aliases cannot be null");
		Map<Long, List<PrincipalAlias>> results = new HashMap<Long, List<PrincipalAlias>>();
		for(PrincipalAlias alias: aliases){
			List<PrincipalAlias> list = results.get(alias.getPrincipalId());
			if(list == null){
				list = new LinkedList<PrincipalAlias>();
				results.put(alias.getPrincipalId(), list);
			}
			list.add(alias);
		}
		return results;
	}
	
	/**
	 * Get the set of principal Ids from the passed list of user profiles.
	 * @param profiles
	 * @return
	 */
	public static Set<Long> getPrincipalIds(List<UserProfile> profiles){
		if(profiles == null) throw new IllegalArgumentException("UserProfiles cannot be null");
		HashSet<Long> set = new HashSet<Long>();
		for(UserProfile profile: profiles){
			if(profile.getOwnerId() == null) throw new IllegalArgumentException("UserProfile.ownerID cannot be null");
			Long princpalId = Long.parseLong(profile.getOwnerId());
			set.add(princpalId);
		}
		return set;
	}
	
	/**
	 * This is used to create a temporary username for users that have not yet set their username.
	 * @return
	 */
	public static String createTempoaryUserName(long principalId){
		return TEMPORARY_USERNAME_PREFIX+principalId;
	}
	
	public static boolean isTempoaryUsername(String username){
		if(username == null) throw new IllegalArgumentException("UserName cannot be null");
		return username.startsWith(TEMPORARY_USERNAME_PREFIX);
	}
}
