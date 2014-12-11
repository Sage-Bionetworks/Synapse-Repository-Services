package org.sagebionetworks.table.query.model;

/**
 * This matches &lt;signed literal&gt; in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SignedLiteral extends SQLElement {

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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (signedNumericLiteral != null) {
			if (columnConvertor != null) {
				columnConvertor.convertNumberParam(signedNumericLiteral, builder);
			} else {
				builder.append(signedNumericLiteral);
			}
		} else {
			if (columnConvertor != null) {
				columnConvertor.convertParam(generalLiteral, builder);
			} else {
				// General literals have single quotes
				builder.append("'");
				builder.append(this.generalLiteral.replaceAll("'", "''"));
				builder.append("'");
			}
		}
	}

}
