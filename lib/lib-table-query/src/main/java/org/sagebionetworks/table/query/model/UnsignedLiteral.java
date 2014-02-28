package org.sagebionetworks.table.query.model;

/**
 * This matches &ltunsigned literal&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class UnsignedLiteral {

	String unsignedNumericLiteral;
	String generalLiteral;
	public UnsignedLiteral(String unsignedNumericLiteral, String generalLiteral) {
		super();
		this.unsignedNumericLiteral = unsignedNumericLiteral;
		this.generalLiteral = generalLiteral;
	}
	public String getUnsignedNumericLiteral() {
		return unsignedNumericLiteral;
	}
	public String getGeneralLiteral() {
		return generalLiteral;
	}
	
}
