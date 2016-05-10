package org.sagebionetworks.table.query.model;

/**
 * Data extracted from a derived column.
 * A derived column can represent many
 * <table>
  <thead>
    <tr><th>SQL</th><th>displayName</th><th>function</th><th>referencedColumnName</th></tr>
  <thead>
  <tbody>
     <tr><td>'constant'</td><td>constant</td><td>null</td><td>constant</td></tr>
  </tbody>
</table>
 */
public class DerivedColumnInfo {
	
	enum Function{
		COUNT,
		MAX,
		MIN,
		SUM,
		AVG,
		FOUND_ROWS
	}

	private String displayName;
	private Function function;
	private String referencedColumnName;
	
	/**
	 * 
	 * @return
	 */
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Function getFunction() {
		return function;
	}
	public void setFunction(Function function) {
		this.function = function;
	}
	public String getReferencedColumnName() {
		return referencedColumnName;
	}
	public void setReferencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}
	
	
	
}
