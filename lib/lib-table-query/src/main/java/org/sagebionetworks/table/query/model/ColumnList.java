package org.sagebionetworks.table.query.model;

import java.util.ArrayList;
import java.util.List;

/**
 * ColumnList ::= <left_paren> {@link Identifier} ( <comma> {@link Identifier}
 * )* <right_paren>
 * 
 * @see <a href=
 *      "http://teiid.github.io/teiid-documents/9.0.x/content/reference/BNF_for_SQL_Grammar.html#columnList">Column
 *      List</a>
 *
 * 
 */
public class ColumnList extends SQLElement implements Replaceable<ColumnList> {

	private final List<Identifier> identifiers;

	public ColumnList() {
		super();
		this.identifiers = new ArrayList<>();
	}
	
	public void addIdentifier(Identifier identifier) {
		this.identifiers.add(identifier);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("(");

		for(int i=0; i< identifiers.size(); i++) {
			if(i >0 ) {
				builder.append(", ");
			}
			identifiers.get(i).toSql(builder, parameters);
		}
		
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(identifiers);
	}

}
