package org.sagebionetworks.repo.web.query;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.Operator;
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
	private String whereTable = null;
	private String whereField = null;
	private Object whereValue = null;
	private String sortTable = null;
	private String sortField = null;
	private List<String> select = null;
	// The list of expressions
	List<Expression> searchCondition = null;
	private Boolean sortAcending = ServiceConstants.DEFAULT_ASCENDING;

	/**
	 * Note that the default is not ServiceConstants.DEFAULT_PAGINATION_LIMIT
	 * like it is for the rest API. When people query they expect that a query
	 * with no limit specified defaults to all.
	 */
	private Long limit = 1000L; // MySQL upper limit
	private Long offset;

	private QueryNode parseTree = null;

	/**
	 * Create a query using the proper definition of the 'offset' pagination param (no offset is zero)
	 * @param query
	 */
	public QueryStatement(String query)  throws ParseException {
		this(query, false);
	}

	/**
	 * @param query
	 * @throws ParseException
	 */
	public QueryStatement(String query, boolean defaultOffset1) throws ParseException {
		if (defaultOffset1) {
			offset = 1L;
		} else {
			offset = ServiceConstants.DEFAULT_PAGINATION_OFFSET;
		}
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
			case QueryParser.JJTSELECT:

				// Get the next part of the select
				QueryNode selectValue = (QueryNode) node.jjtGetChild(0);
				if("*".equals(selectValue.jjtGetValue())){
					// This is a select *
					this.select = null;
				}else{
					// We have a select list
					this.select = new ArrayList<String>();
					QueryNode selectList = (QueryNode) selectValue.jjtGetChild(0);
					for(int selectIndex=0; selectIndex<selectList.jjtGetNumChildren(); selectIndex++){
						QueryNode name = (QueryNode) selectList.jjtGetChild(selectIndex);
						this.select.add((String) name.jjtGetValue());
					}
				}
				break;
			case QueryParser.JJTSELECTLIST:
				QueryNode nameValue = (QueryNode) node.jjtGetChild(0);
				this.select.add((String)nameValue.jjtGetValue());
				break;
			case QueryParser.JJTWHERE:
				QueryNode whereFieldNode = (QueryNode) node.jjtGetChild(0);
				if(QueryParser.JJTSEARCHCONDITION == whereFieldNode.getId()){
					// This list is the infix representation of the search condition.
					searchCondition = new ArrayList<Expression>();
					// Process each child
					for(int j=0; j< whereFieldNode.jjtGetNumChildren();j++){
						QueryNode child = (QueryNode) whereFieldNode.jjtGetChild(j);
						if(QueryParser.JJTEXPRESSION == child.getId()){
							// Create our Expression
							Expression expression = parseExpression(child);
							searchCondition.add(expression);
							
						}else if(QueryParser.JJTOPERATOR == child.getId()){
							// What is the comparator?
							Operator operator = (Operator)child.jjtGetValue();
							// Currently the only valid operator is and
							if(Operator.AND != operator) throw new IllegalArgumentException("Currenlty, only the 'and' opperator is supported.");
						}else{
							 throw new IllegalArgumentException("Encountered: "+child.jjtGetValue()+" but was expecting: exression or operator");
						}
					}
				}
				break;
			case QueryParser.JJTORDERBY:
				QueryNode sortFieldNode = (QueryNode) node.jjtGetChild(0);
				if (QueryParser.JJTCOMPOUNDID == sortFieldNode.getId()) {
					if(2 == sortFieldNode.jjtGetNumChildren()) {
						sortTable= (String) ((QueryNode) sortFieldNode.jjtGetChild(0))
						.jjtGetValue();
						sortField = (String) ((QueryNode) sortFieldNode.jjtGetChild(1))
						.jjtGetValue();
					}
					else {
						sortField = (String) ((QueryNode) sortFieldNode.jjtGetChild(0))
						.jjtGetValue();
					}
				} else {
					sortField = (String) ((QueryNode) node.jjtGetChild(0))
							.jjtGetValue();
				}
				if (1 < node.jjtGetNumChildren()) {
					sortAcending = (Boolean) ((QueryNode) node.jjtGetChild(1))
							.jjtGetValue();
				}
				break;
			case QueryParser.JJTLIMIT:
				Long newLimit = (Long) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				// If we overflow, our validation below will raise an error
				limit = newLimit;
				break;
			case QueryParser.JJTOFFSET:
				Long newOffset = (Long) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				// If we overflow, our validation below will raise an error
				offset = newOffset;
				break;
			}
		}
		ServiceConstants.validatePaginationParams(offset, limit);
		if (defaultOffset1 && offset < 1) {
			throw new IllegalArgumentException("This API requires offset to be 1 or greater.");
		}
	}
	
	/**
	 * Pull an expression from the tree
	 * @param expressionNode
	 * @return
	 */
	private Expression parseExpression(QueryNode expressionNode){
		// The Expressions always have three parts
		QueryNode idNode = (QueryNode) expressionNode.jjtGetChild(0);
		CompoundId id = parseId(idNode);
		QueryNode compareNode = (QueryNode) expressionNode.jjtGetChild(1);
		Comparator comp = (Comparator)compareNode.jjtGetValue();
		QueryNode valueNode = (QueryNode) ((QueryNode) expressionNode.jjtGetChild(2)).jjtGetChild(0);
		Object value = valueNode.jjtGetValue();
		return new Expression(id, comp, value);
	}
	
	/**
	 * Pull a CompoundId from the tree
	 * @param idNode
	 * @return
	 */
	private CompoundId parseId(QueryNode idNode){
		
		String table = null;
		String field = null;
		if(2 == idNode.jjtGetNumChildren()) {
			table= (String) ((QueryNode) idNode.jjtGetChild(0)).jjtGetValue();
			field  = (String) ((QueryNode) idNode.jjtGetChild(1))
			.jjtGetValue();
		}else if (1 == idNode.jjtGetNumChildren()){
			field = (String) ((QueryNode) idNode.jjtGetChild(0))
			.jjtGetValue();
		}else{
			field = (String) idNode.jjtGetValue();
		}
		return new CompoundId(table, field);
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @return the whereTable
	 */
	public String getWhereTable() {
		return whereTable;
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
	 * The select clause.
	 * @return
	 */
	public List<String> getSelect(){
		return this.select;
	}

	/**
	 * @return the search conditions
	 */
	public List<Expression> getSearchCondition() {
		return searchCondition;
	}

	/**
	 * @return the sortTable
	 */
	public String getSortTable() {
		return sortTable;
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
	public Long getLimit() {
		return limit;
	}

	/**
	 * @return the offset
	 */
	public Long getOffset() {
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