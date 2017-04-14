package org.sagebionetworks.repo.model.query.jdo.modelv2;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.BasicQueryUtils;

public class QueryModel {
	
	boolean selectStar;
	boolean selectAnnotation;
	List<Column> select;
	List<ModelExpression> where;
	
	/**
	 * Build a query model for a given query.
	 * 
	 * @param query
	 */
	public QueryModel(BasicQuery query){
		// this method uses the converted from of the query.
		query = BasicQueryUtils.convertFromToExpressions(query);
		// build select
		buildSelect(query);
		// build where
		buildWhere(query);
	}

	/**
	 * Build the where clause from the query.
	 * s
	 * @param query
	 */
	void buildWhere(BasicQuery query) {
		where = new LinkedList<ModelExpression>();
		if (query.getFilters() != null) {
			for (Expression expression : query.getFilters()) {
				if (expression.getId() != null
						&& expression.getId().getFieldName() != null) {
					String fieldName = expression.getId().getFieldName();
					try {
						NodeToEntity type = NodeToEntity.valueOf(fieldName);
						// this is a node field
						where.add(new ModelExpression(
								new NodeColumn(type),
								expression.getCompare(),
								expression.getValue()));
					} catch (IllegalArgumentException e) {
						// this is an annotation
						where.add(new ModelExpression(
								new AnnotationColumn(fieldName),
								expression.getCompare(),
								expression.getValue()));
					}
				}
			}
		}
	}

	/**
	 * Build the select clause from the passed query.
	 * @param query
	 */
	void buildSelect(BasicQuery query) {
		select = new LinkedList<>();
		selectStar = false;
		selectAnnotation = false;
		if(query.getSelect() == null || query.getSelect().isEmpty()){
			selectStar = true;
			// add all of the node fields
			for(NodeToEntity nodeToEntity: NodeToEntity.values()){
				select.add(new NodeColumn(nodeToEntity));
			}
		}else{
			// Add the columns selected by the users
			for(String inSelect: query.getSelect()){
				try{
					NodeToEntity type = NodeToEntity.valueOf(inSelect);
					select.add(new NodeColumn(type));
				}catch(IllegalArgumentException e){
					// this is an annotation
					selectAnnotation = true;
					select.add(new AnnotationColumn(inSelect));
				}
			}
		}
	}
	

}
