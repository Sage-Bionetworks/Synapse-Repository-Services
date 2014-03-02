package org.sagebionetworks.table.query.model;

/**
 * This matches &ltunsigned literal&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class UnsignedLiteral implements SQLElement{

	String unsignedNumericLiteral;
	String generalLiteral;
	public UnsignedLiteral(String unsignedNumericLiteral, String generalLiteral) {
		super();
		this.unsignedNumericLiteral = unsignedNumericLiteral;
		this.generalLiteral = generalLiteral;
		if(unsignedNumericLiteral != null && generalLiteral != null) throw new IllegalArgumentException("An UnsignedLiteral can be an UnsignedNumericLiteral or a GeneralLiteral but not both");
	}
	public String getUnsignedNumericLiteral() {
		return unsignedNumericLiteral;
	}
	public String getGeneralLiteral() {
		return generalLiteral;
	}
	@Override
	public void toSQL(StringBuilder builder) {
		if(unsignedNumericLiteral != null){
			builder.append(unsignedNumericLiteral);
		}else{
			// General literals have single quotes
			builder.append("'");
			builder.append(this.generalLiteral.replaceAll("'", "''"));
			builder.append("'");
		}
	}
	
}
