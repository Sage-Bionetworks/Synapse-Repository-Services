package org.sagebionetworks.repo.model.dbo.dao;

public class ListQueryUtils {
	
	public static String bindVariable(int i) {
		return "p"+i;
	}
	
	public static String selectListInClause(final int n) {
		if (n<1) throw new IllegalArgumentException("Expected n>0 but found "+n);
		StringBuilder sb = new StringBuilder(" IN (");
		for (int i=0; i<n; i++) {
			if (i>0) sb.append(",");
			sb.append(":"+bindVariable(i));
		}
		sb.append(")");
		return sb.toString();
	}	

}
