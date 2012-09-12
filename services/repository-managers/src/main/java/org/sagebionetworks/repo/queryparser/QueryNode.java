/**
 * 
 */
package org.sagebionetworks.repo.queryparser;

/**
 * @author deflaux
 *
 */
public class QueryNode extends SimpleNode {

	/**
	 * @param i
	 */
	public QueryNode(int i) {
		super(i);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param p
	 * @param i
	 */
	public QueryNode(QueryParser p, int i) {
		super(p, i);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Get the numeric id of this node so that we know what type it is.
	 * 
	 * @return the numeric id
	 */
	public int getId() {
		return id;
	}

}
