package org.sagebionetworks.table.query.model;

/**
 * This matches &lt;signed literal&gt; in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SignedLiteral implements SQLElement{

	String signedNumericLiteral;
	String generalLiteral;

	public SignedLiteral(String signedNumericLiteral, String generalLiteral) {
		super();
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
	public void toSQL(StringBuilder builder) {
		if (signedNumericLiteral != null) {
			builder.append(signedNumericLiteral);
		}else{
			// General literals have single quotes
			builder.append("'");
			builder.append(this.generalLiteral.replaceAll("'", "''"));
			builder.append("'");
		}
	}
	
}
