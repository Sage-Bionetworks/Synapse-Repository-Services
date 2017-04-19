package org.sagebionetworks.repo.model.query.entity;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the select list of an entity query.
 *
 */
public class SelectList extends SqlElement {

	boolean isSelectStar;
	boolean includesAnnotation;
	List<SelectColumn> select;
	
	/**
	 * Create a select list from the list of input strings
	 * 
	 * @param inputSelect
	 */
	public SelectList(List<String> inputSelect){
		select = new LinkedList<>();
		isSelectStar = false;
		includesAnnotation = false;
		if(inputSelect == null || inputSelect.isEmpty()){
			isSelectStar = true;
			// add all of the node fields
			for(NodeToEntity nodeToEntity: NodeToEntity.values()){
				select.add(new SelectColumn(nodeToEntity));
			}
		}else{
			// Add the columns selected by the users
			for(String inSelect: inputSelect){
				try{
					NodeToEntity type = NodeToEntity.valueOf(inSelect);
					select.add(new SelectColumn(type));
				}catch(IllegalArgumentException e){
					// this is an annotation
					includesAnnotation = true;
					select.add(new SelectColumn(inSelect));
				}
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

	/**
	 * Does the select include any annotations?
	 * s
	 * @return
	 */
	public boolean includesAnnotations() {
		return includesAnnotation;
	}


	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}
	
}
