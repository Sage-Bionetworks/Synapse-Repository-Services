package org.sagebionetworks.repo.manager.grid.row.translator;

import org.json.JSONException;

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

	default ConValue translateLeniently(String string) {
		return translateLeniently(string, true);
	}

	/**
	 * Like {@link #translateNullable(String, boolean)}, but never throws: a value
	 * this translator's type cannot represent is translated as
	 * {@link ConType#STRING} rather than failing the caller.
	 * <p>
	 * A blank value is treated as no value for column types whose translator cannot
	 * represent one. Types that accept any string — STRING, BOOLEAN and the list
	 * types — translate a blank value exactly as
	 * {@link #translateNullable(String, boolean)} does, because leniency never
	 * alters a translation that already succeeds.
	 *
	 * @param string             the raw value; {@code null} is treated as no value.
	 * @param isRequiredProperty whether a no-value result should be
	 *                           {@link ConType#NULL} (required) or
	 *                           {@link ConType#UNDEFINED} (not required).
	 */
	default ConValue translateLeniently(String string, boolean isRequiredProperty) {
		// Only the exceptions the translators actually throw are tolerated: a parse
		// failure is an IllegalArgumentException (NumberFormatException, Joda's date
		// parser) and org.json throws JSONException. A translator defect such as an
		// NPE must still fail loudly rather than produce a text cell.
		try {
			return translateNullable(string, isRequiredProperty);
		} catch (IllegalArgumentException | JSONException e) {
			// This blank check MUST stay inside the catch rather than guarding the try:
			// it must apply only to types whose translator rejects a blank value.
			// Hoisting it would turn a blank STRING cell from "" into no value, which
			// changes the row hash that RowSourceItem#getHash feeds to the sync's
			// change detection, so unchanged rows would be reported as changed.
			if (string == null || string.trim().isEmpty()) {
				return translateNullable(null, isRequiredProperty);
			}
			return new ConValue(ConType.STRING, string);
		}
	}

}
