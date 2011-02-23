package org.sagebionetworks.repo.web;

import java.io.StringReader;

import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.queryparser.QueryNode;
import org.sagebionetworks.repo.queryparser.QueryParser;
import org.sagebionetworks.repo.queryparser.TokenMgrError;

/**
 * QueryStatement encapsulates the logic that extracts values from the parse
 * tree for use in persistence layer queries.
 * 
 * See src/main/jjtree/query.jjt for the little query language definition in BNF
 * 
 * @author deflaux
 * 
 */
public class QueryStatement {

	private String tableName = null;
	private String whereField = null;
	private Object whereValue = null;
	private String sortField = null;
	private Boolean sortAcending = ServiceConstants.DEFAULT_ASCENDING;

	/**
	 * Note that the default is not ServiceConstants.DEFAULT_PAGINATION_LIMIT
	 * like it is for the rest API. When people query they expect that a query
	 * with no limit specified defaults to all.
	 */
	private Integer limit = Integer.MAX_VALUE;
	private Integer offset = ServiceConstants.DEFAULT_PAGINATION_OFFSET;

	private QueryNode parseTree = null;

	/**
	 * @param query
	 * @throws ParseException
	 */
	public QueryStatement(String query) throws ParseException {

		// TODO stash this in ThreadLocal because its expensive to create and
		// not threadsafe
		try {
			QueryParser parser = new QueryParser(new StringReader(query));
			parseTree = (QueryNode) parser.Start();
		} catch (TokenMgrError error) {
			// TokenMgrError is a runtime error but it is not fatal, catching
			// and re-throwing here so that it can be properly handled upstream
			// TODO ParseException currently does not have a constructor that
			// takes the cause object, see if we can override it
			throw new ParseException("TokenMgrError: " + error.getMessage());
		}

		for (int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch (node.getId()) {
			case QueryParser.JJTTABLENAME:
				tableName = (String) node.jjtGetValue();
				break;
			case QueryParser.JJTWHERE:
				QueryNode whereFieldNode = (QueryNode) node.jjtGetChild(0);
				if (QueryParser.JJTCOMPOUNDID == whereFieldNode.getId()) {
					// TODO later we might want to return these as two separate fields table and column
					whereField = (String) ((QueryNode) whereFieldNode.jjtGetChild(0))
					.jjtGetValue();
					if(2 == whereFieldNode.jjtGetNumChildren()) {
						whereField = whereField + "." + ((QueryNode) whereFieldNode.jjtGetChild(1))
						.jjtGetValue();
					}
				} else {
					whereField = (String) ((QueryNode) node.jjtGetChild(0))
							.jjtGetValue();
				}
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0))
						.jjtGetValue();
				break;
			case QueryParser.JJTORDERBY:
				sortField = (String) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				if (1 < node.jjtGetNumChildren()) {
					sortAcending = (Boolean) ((QueryNode) node.jjtGetChild(1))
							.jjtGetValue();
				}
				break;
			case QueryParser.JJTLIMIT:
				Long newLimit = (Long) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				// If we overflow, our validation below will raise an error
				limit = newLimit.intValue();
				break;
			case QueryParser.JJTOFFSET:
				Long newOffset = (Long) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				// If we overflow, our validation below will raise an error
				offset = newOffset.intValue();
				break;
			}
		}
		ServiceConstants.validatePaginationParams(offset, limit);
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @return the whereField
	 */
	public String getWhereField() {
		return whereField;
	}

	/**
	 * @return the whereValue
	 */
	public Object getWhereValue() {
		return whereValue;
	}

	/**
	 * @return the sortField
	 */
	public String getSortField() {
		return sortField;
	}

	/**
	 * @return the sortAcending
	 */
	public Boolean getSortAcending() {
		return sortAcending;
	}

	/**
	 * @return the limit
	 */
	public Integer getLimit() {
		return limit;
	}

	/**
	 * @return the offset
	 */
	public Integer getOffset() {
		return offset;
	}

	/**
	 * Helper method for unit tests and debugging tasks.
	 * <p>
	 * 
	 * If parsing completed without exceptions, print the resulting parse tree
	 * on standard output.
	 */
	public void dumpParseTree() {
		parseTree.dump("");
	}
}