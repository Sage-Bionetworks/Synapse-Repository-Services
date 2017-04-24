package org.sagebionetworks.repo.model.query.entity;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the select list of an entity query.
 *
 */
public class SelectList extends SqlElement implements HasAnnotationReference {

	boolean isSelectStar;
	List<SelectColumn> select;
	
	/**
	 * Create a select list from the list of input strings
	 * 
	 * @param inputSelect
	 */
	public SelectList(List<String> inputSelect, IndexProvider indexProvider){
		select = new LinkedList<>();
		isSelectStar = false;
		if(inputSelect == null || inputSelect.isEmpty()){
			isSelectStar = true;
			// add all of the node fields
			for(NodeToEntity nodeToEntity: NodeToEntity.values()){
				ColumnReference ref = new ColumnReference(nodeToEntity.nodeField.getFieldName(), indexProvider.nextIndex());
				select.add(new SelectColumn(ref));
			}
		}else{
			// Add the columns selected by the users
			for(String inSelect: inputSelect){
				ColumnReference ref = new ColumnReference(inSelect, indexProvider.nextIndex());
				select.add(new SelectColumn(ref));
			}
		}
	}
	
	
	@Override
	public void toSql(StringBuilder builder) {
		builder.append("SELECT ");
		boolean first = true;
		for(SelectColumn column: select){
			if(!first){
				builder.append(", ");
			}
			column.toSql(builder);
			first = false;
		}		
	}


	/**
	 * Was this a select start query?
	 * @return
	 */
	public boolean isSelectStar() {
		return isSelectStar;
	}

	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}
	
	@Override
	public List<ColumnReference> getAnnotationReferences() {
		List<ColumnReference> results = new LinkedList<ColumnReference>();
		for(SelectColumn column: select){
			if(column.columnReference.getAnnotationAlias() != null){
				results.add(column.columnReference);
			}
		}
		return results;
	}
}
