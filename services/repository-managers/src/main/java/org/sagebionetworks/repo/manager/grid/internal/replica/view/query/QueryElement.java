package org.sagebionetworks.repo.manager.grid.internal.replica.view.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterTranslation;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectAllElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectItemElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select.SelectItemTranslator;
import org.sagebionetworks.repo.model.grid.query.Query;

public class QueryElement implements Element {
	
	private static void appendElementList(StringBuilder sqlBuilder, Map<String, Object> params, Context context, List<? extends Element> elements, String separator) {
		boolean isFirst = true;
		for (Element element : elements) {
			if (!isFirst) {
				sqlBuilder.append(separator);
			}
			element.toSql(sqlBuilder, params, context);
			isFirst = false;
		}
	}

	private List<SelectItemElement> select;
	private List<FilterElement> where;
	private List<ColumnNameElement> groupBy;
	private List<OrderByItemElement> orderBy;
	private Long limit;
	private Long offset;

	public QueryElement() {
		select = List.of(new SelectAllElement());
	}

	public QueryElement(Query query) {
		select = query.getColumnSelection() != null ? query.getColumnSelection().stream()
				.map(SelectItemTranslator::translate)
				.collect(Collectors.toList()) : List.of(new SelectAllElement());
		
		if (query.getFilters() != null) {
			where = query.getFilters().stream().map(FilterTranslation::translate).collect(Collectors.toList());
		}
		
		this.limit = query.getLimit();
		this.offset = query.getOffset();
	}

	@Override
	public void toSql(StringBuilder sqlBuilder, Map<String, Object> params, Context context) {
		sqlBuilder.append("SELECT");
		
		if (!isAggregate()) {
			// For non-aggregate queries, always include all metadata columns that are needed to map a row into a RowView
			sqlBuilder.append(" AN_REP, AN_SEQ, `INDEX`, RO_REP, RO_SEQ, VEC_REP, VEC_SEQ,");
			sqlBuilder.append(" MO_REP, MO_SEQ, SRC_REP, SRC_SEQ, RVC_REP, RVC_SEQ, VAL_RES, SYN_ROW,");
		}
			
		// Build a SELECTED_VALS sub-array from the selected columns extracted directly from the CTE VALS array
		sqlBuilder.append(" JSON_ARRAY(");
		
		appendElementList(sqlBuilder, params, context, select, ",");
		
		sqlBuilder.append(") AS SELECTED_VALS");
	
		sqlBuilder.append(" FROM GRID");
		if (where != null && !where.isEmpty()) {
			sqlBuilder.append(" WHERE");
			appendElementList(sqlBuilder, params, context, where, " AND");
		}
		if (groupBy != null) {
			sqlBuilder.append(" GROUP BY");
			appendElementList(sqlBuilder, params, context, groupBy, ",");
		}
		sqlBuilder.append(" ORDER BY");
		if (orderBy != null) {
			appendElementList(sqlBuilder, params, context, orderBy, ",");
		} else {
			sqlBuilder.append(" `INDEX` ASC");
		}
		if (limit != null) {
			sqlBuilder.append(" LIMIT :limit");
			params.put("limit", limit);
		}
		if (offset != null) {
			sqlBuilder.append(" OFFSET :offset");
			params.put("offset", offset);
		}
	}

	public List<SelectItemElement> getSelect() {
		return select;
	}

	public QueryElement setSelect(List<SelectItemElement> select) {
		this.select = select;
		return this;
	}

	public QueryElement setSelect(SelectItemElement... item) {
		this.select = Arrays.asList(item);
		return this;
	}

	public List<FilterElement> getWhere() {
		return where;
	}

	public QueryElement setWhere(List<FilterElement> where) {
		this.where = where;
		return this;
	}

	public List<ColumnNameElement> getGroupBy() {
		return groupBy;
	}

	public QueryElement setGroupBy(List<ColumnNameElement> groupBy) {
		this.groupBy = groupBy;
		return this;
	}

	public List<OrderByItemElement> getOrderBy() {
		return orderBy;
	}

	public QueryElement setOrderBy(List<OrderByItemElement> orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public Long getLimit() {
		return limit;
	}

	public QueryElement setLimit(Long limit) {
		this.limit = limit;
		return this;
	}

	public Long getOffset() {
		return offset;
	}

	public QueryElement setOffset(Long offset) {
		this.offset = offset;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupBy, limit, offset, orderBy, select, where);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryElement other = (QueryElement) obj;
		return Objects.equals(groupBy, other.groupBy) && Objects.equals(limit, other.limit)
				&& Objects.equals(offset, other.offset) && Objects.equals(orderBy, other.orderBy)
				&& Objects.equals(select, other.select) && Objects.equals(where, other.where);
	}

	@Override
	public String toString() {
		return "QueryElement [select=" + select + ", where=" + where + ", groupBy=" + groupBy + ", orderBy=" + orderBy
				+ ", limit=" + limit + ", offset=" + offset + "]";
	}

	public boolean isAggregate() {
		return select.stream().filter(SelectItemElement::isAggregate).findFirst().isPresent();
	}

}
