package org.sagebionetworks.repo.model.query.entity;

import static org.sagebionetworks.repo.model.table.TableConstants.ENTITY_REPLICATION_TABLE;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents tables and joins for a query.
 * 
 *
 */
public class Tables extends SqlElement {
	
	List<AnnotationJoin> annotationExpressions;

	public Tables(SelectList select, ExpressionList list, SortList sortList){
		annotationExpressions = new LinkedList<AnnotationJoin>();
		
		for(ColumnReference annotationRef: select.getAnnotationReferences()){
			// left join is used for annotations in a select.
			boolean leftJoin = true;
			AnnotationJoin join = new AnnotationJoin(annotationRef, leftJoin);
			annotationExpressions.add(join);
		}
		// look for the annotation expressions
		for(ColumnReference annotationRef: list.getAnnotationReferences()){
			// Annotation expression are filters so inner join is used
			boolean leftJoin = false;
			AnnotationJoin join = new AnnotationJoin(annotationRef, leftJoin);
			annotationExpressions.add(join);
		}
		// An annotation join is needed to sort on an annotation.
		if(sortList.sortColumn != null && sortList.sortColumn.getAnnotationAlias() != null){
			// sorting should not filter, so left join is used.
			boolean leftJoin = true;
			AnnotationJoin join = new AnnotationJoin(sortList.sortColumn, leftJoin);
			annotationExpressions.add(join);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(" FROM ");
		builder.append(ENTITY_REPLICATION_TABLE);
		builder.append(" ");
		builder.append(Constants.ENTITY_REPLICATION_ALIAS);
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
