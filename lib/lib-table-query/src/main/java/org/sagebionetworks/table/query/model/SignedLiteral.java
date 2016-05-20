package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &lt;signed literal&gt; in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SignedLiteral extends SQLElement implements HasQuoteValue {

	String signedNumericLiteral;
	String generalLiteral;

	public SignedLiteral(String signedNumericLiteral, String generalLiteral) {
		this.signedNumericLiteral = signedNumericLiteral;
		this.generalLiteral = generalLiteral;
		if (signedNumericLiteral != null && generalLiteral != null)
			throw new IllegalArgumentException("An SignedLiteral can be an SignedNumericLiteral or a GeneralLiteral but not both");
	}

	public String getSignedNumericLiteral() {
		return signedNumericLiteral;
	}

	public String getGeneralLiteral() {
		return generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if (signedNumericLiteral != null) {
			builder.append(signedNumericLiteral);
		} else {
			// General literals have single quotes
			builder.append("'");
			builder.append(this.generalLiteral.replaceAll("'", "''"));
			builder.append("'");
		}
	}

	@Override
	public String getValueWithoutQuotes() {
		if (signedNumericLiteral != null) {
			return signedNumericLiteral;
		} else {
			return generalLiteral;
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}

	@Override
	public boolean isSurrounedeWithQuotes() {
		return generalLiteral != null;
	}

	@Override
	public void replaceUnquoted(String newValue) {
		signedNumericLiteral = newValue;
		generalLiteral = null;
	}
	
}
