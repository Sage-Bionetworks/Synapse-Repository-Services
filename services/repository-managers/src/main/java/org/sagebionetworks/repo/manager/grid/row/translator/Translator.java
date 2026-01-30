package org.sagebionetworks.repo.manager.grid.row.translator;

import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Translation from a String to a {@link ConValue}.
 */
public interface Translator {

	default ConValue translateNullable(String string) {
		return translateNullable(string, true);
	}

	default ConValue translateNullable(String string, boolean isRequiredProperty) {
		if (string == null) {
			if (isRequiredProperty) {
				return new ConValue(ConType.NULL, null);
			}
			return new ConValue(ConType.UNDEFINED, null);
		}
		return translate(string);
	}

	/**
	 * Translate from a String to ConValue
	 * 
	 * @param string
	 * @return
	 */
	ConValue translate(String string);


}
