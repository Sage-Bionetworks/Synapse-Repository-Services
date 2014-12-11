package org.sagebionetworks.repo.model.jdo;


/**
 * This class is derived from
 * org.datanucleus.store.mapped.identifier.JPOXIdentifierFactory
 * 
 * If anyone can figure out how to get the instance of JPOXIdentifierFactory
 * used by datanucleus, then we should switch to that.
 * 
 * @author jmhill
 * 
 */
public class BasicIdentifierFactory {

	public enum IdentifierCase {
		UPPER_CASE("UPPERCASE"),
		UPPER_CASE_QUOTED("\"UPPERCASE\""),
		LOWER_CASE("lowercase"),
		LOWER_CASE_QUOTED("\"lowercase\""),
		MIXED_CASE("MixedCase"),
		MIXED_CASE_QUOTED("\"MixedCase\"");

		String name;

		private IdentifierCase(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	public static BasicIdentifierFactory instances = null;
	IdentifierCase identifierCase = IdentifierCase.UPPER_CASE;
	String wordSeparator = "_";

	public IdentifierCase getIdentifierCase() {
		return identifierCase;
	}

	public void setIdentifierCase(IdentifierCase identifierCase) {
		this.identifierCase = identifierCase;
	}

	public String getWordSeparator() {
		return wordSeparator;
	}

	public void setWordSeparator(String wordSeparator) {
		this.wordSeparator = wordSeparator;
	}

	/**
	 * @see org.datanucleus.store.mapped.identifier.JPOXIdentifierFactory#generateIdentifierNameForJavaName(String)
	 * @param javaName
	 * @return
	 */
	public String generateIdentifierNameForJavaName(String javaName) {
		if (javaName == null) {
			return null;
		}

		StringBuilder s = new StringBuilder();
		char prev = '\0';

		for (int i = 0; i < javaName.length(); ++i) {
			char c = javaName.charAt(i);

			if (c >= 'A'
					&& c <= 'Z'
					&& (identifierCase != IdentifierCase.MIXED_CASE && identifierCase != IdentifierCase.MIXED_CASE_QUOTED)) {
				if (prev >= 'a' && prev <= 'z') {
					s.append(wordSeparator);
				}

				s.append(c);
			} else if (c >= 'A'
					&& c <= 'Z'
					&& (identifierCase == IdentifierCase.MIXED_CASE || identifierCase == IdentifierCase.MIXED_CASE_QUOTED)) {
				s.append(c);
			} else if (c >= 'a'
					&& c <= 'z'
					&& (identifierCase == IdentifierCase.MIXED_CASE || identifierCase == IdentifierCase.MIXED_CASE_QUOTED)) {
				s.append(c);
			} else if (c >= 'a'
					&& c <= 'z'
					&& (identifierCase != IdentifierCase.MIXED_CASE && identifierCase != IdentifierCase.MIXED_CASE_QUOTED)) {
				s.append((char) (c - ('a' - 'A')));
			} else if (c >= '0' && c <= '9' || c == '_') {
				s.append(c);
			} else if (c == '.') {
				s.append(wordSeparator);
			} else {
				String cval = "000" + Integer.toHexString(c);

				s.append(cval.substring(cval.length() - (c > 0xff ? 4 : 2)));
			}

			prev = c;
		}

		// Remove leading and trailing underscores
		while (s.length() > 0 && s.charAt(0) == '_') {
			s.deleteCharAt(0);
		}
		if (s.length() == 0) {
			throw new IllegalArgumentException("Illegal Java identifier: "
					+ javaName);
		}

		return s.toString();
	}

}
