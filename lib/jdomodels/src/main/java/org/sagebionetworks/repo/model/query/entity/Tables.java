package org.sagebionetworks.repo.model.query.entity;

import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.repo.model.table.TableConstants.*;

/**
 * Represents tables and joins for a query.
 * 
 *
 */
public class Tables extends SqlElement {
	
	List<AnnotationJoin> annotationExpressions;

	public Tables(ExpressionList list, SortList sortList){
		annotationExpressions = new LinkedList<AnnotationJoin>();
		// look for the annotation expressions
		for(SqlExpression annoExpression: list.getAnnotationExpressions()){
			AnnotationJoin join = new AnnotationJoin(annoExpression.leftHandSide);
			annotationExpressions.add(join);
		}
		// An annotation join is needed to sort on an annotation.
		if(sortList.sortColumn != null && sortList.sortColumn.getAnnotationAlias() != null){
			AnnotationJoin join = new AnnotationJoin(sortList.sortColumn);
			annotationExpressions.add(join);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(" FROM ");
		builder.append(ENTITY_REPLICATION_TABLE);
		builder.append(" R");
		// add a join for each annotation expression
		for(AnnotationJoin join: annotationExpressions){
			join.toSql(builder);
		}		
	}

	@Override
	public void bindParameters(Parameters parameters) {
		for(AnnotationJoin join: annotationExpressions){
			join.bindParameters(parameters);
		}	
	}
}
