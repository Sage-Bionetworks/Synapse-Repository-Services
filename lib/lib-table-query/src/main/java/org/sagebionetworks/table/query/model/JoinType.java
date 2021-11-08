package org.sagebionetworks.table.query.model;

/**
 * From &ltjoin type&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 * <p>
 * Effectively:
 * <p>
 * &ltjoin type&gt ::= INNER | &ltouter join type&gt [OUTER]
 *
 */
public class JoinType extends LeafElement {

	private final boolean explictInner;
	private final OuterJoinType outerJoinType;
	private final boolean explicitOutter;
	
	public JoinType() {
		super();
		this.explictInner = true;
		this.outerJoinType = null;
		this.explicitOutter = false;
	}

	public JoinType(OuterJoinType outerJoinType, boolean explicitOutter) {
		super();
		this.explictInner = false;
		this.outerJoinType = outerJoinType;
		this.explicitOutter = explicitOutter;
	}

	public JoinType(OuterJoinType outerJoinType) {
		this(outerJoinType, false);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if (this.explictInner) {
			builder.append("INNER");
		} else {
			builder.append(outerJoinType.name());
			if (explicitOutter) {
				builder.append(" OUTER");
			}
		}
	}

}
