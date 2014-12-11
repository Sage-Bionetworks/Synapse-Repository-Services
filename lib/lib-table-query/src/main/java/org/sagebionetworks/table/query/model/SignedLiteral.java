package org.sagebionetworks.table.query.model;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.visitors.ColumnTypeVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToUnquotedStringVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public void visit(Visitor visitor) {
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if (signedNumericLiteral != null) {
			visitor.append(signedNumericLiteral);
		} else {
			// General literals have single quotes
			visitor.append("'");
			visitor.append(this.generalLiteral.replaceAll("'", "''"));
			visitor.append("'");
		}
	}

	public void visit(ToUnquotedStringVisitor visitor) {
		if (signedNumericLiteral != null) {
			visitor.append(signedNumericLiteral);
		} else {
			visitor.append(this.generalLiteral);
		}
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		if (signedNumericLiteral != null) {
			visitor.convertNumberParam(signedNumericLiteral);
		} else {
			visitor.convertParam(generalLiteral);
		}
	}

	public void visit(ColumnTypeVisitor visitor) {
		if (signedNumericLiteral != null) {
			visitor.setColumnType(ColumnType.DOUBLE);
		} else {
			visitor.setColumnType(ColumnType.STRING);
		}
	}
}
