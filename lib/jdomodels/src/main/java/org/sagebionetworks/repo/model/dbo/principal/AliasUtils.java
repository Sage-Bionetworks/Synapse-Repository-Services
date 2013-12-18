package org.sagebionetworks.repo.model.dbo.principal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.principal.PrincipalAlias;

/**
 * Utilities for alias.
 * @author John
 *
 */
public class AliasUtils {
	
	public static DBOPrincipalAlias createDBOFromDTO(PrincipalAlias dto){
		DBOPrincipalAlias dbo = new DBOPrincipalAlias();
		// First validate the name type.
		
		
		return dbo;
	}
	
	// Used to replace all characters expect letters and numbers.
	private static Pattern PRINICPAL_UNIQUENESS_REPLACE_PATTERN = Pattern
			.compile("[^a-z0-9]");

	/**
	 * Get the string that will be used for a uniqueness check for alias
	 * names. Only lower case letters and numbers contribute to the uniqueness
	 * of a principal name. All other characters (-,., ,_) are ignored.
	 * 
	 * @param inputName
	 * @return
	 */
	public static String getUniqueAliasName(String inputName) {
		if (inputName == null)
			throw new IllegalArgumentException("Name cannot be null");
		// Case does not contribute to uniqueness
		String lower = inputName.toLowerCase();
		// Only letters and numbers contribute to the uniqueness
		Matcher m = PRINICPAL_UNIQUENESS_REPLACE_PATTERN.matcher(lower);
		// Replace all non-letters and numbers with empty strings
		return m.replaceAll("");
	}

}
