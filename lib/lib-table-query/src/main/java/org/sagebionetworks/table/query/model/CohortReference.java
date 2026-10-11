package org.sagebionetworks.table.query.model;

import java.util.regex.Pattern;

/**
 * A reference to a request-scoped cohort, the right-hand side of {@code <column> IN COHORT(<name>)}. A
 * cohort reference must be replaced with the cohort's values before the query is translated.
 */
public class CohortReference extends SQLElement {

	private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

	private final RegularIdentifier name;

	/**
	 * @return true when the given name is a simple identifier, the only form a cohort name may take.
	 */
	public static boolean isValidName(String name) {
		return name != null && NAME_PATTERN.matcher(name).matches();
	}

	public CohortReference(RegularIdentifier name) {
		this.name = name;
	}

	/**
	 * @return the name of the referenced cohort.
	 */
	public String getName() {
		return name.toSql();
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("COHORT(");
		name.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(name);
	}

}
