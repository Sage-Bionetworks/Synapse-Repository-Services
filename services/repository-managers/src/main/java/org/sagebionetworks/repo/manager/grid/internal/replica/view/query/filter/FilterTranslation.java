package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import java.util.function.Function;

import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.Filter;
import org.sagebionetworks.repo.model.grid.query.RowIdFilter;
import org.sagebionetworks.repo.model.grid.query.RowIsValidFilter;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.repo.model.grid.query.RowValidationResultFilter;

public enum FilterTranslation {

	CellValue(CellValueFilter.class, CellValueFilterElement::new),
	RowSelection(RowSelectionFilter.class, RowSelectionFilterElement::new),
	RowValidationResult(RowValidationResultFilter.class, RowValidationResultFilterElement::new),
	RowIsValid(RowIsValidFilter.class, RowIsValidFilterElement::new),
	RowId(RowIdFilter.class, VectorIdFilterElement::new);

	private final Class<? extends Filter> filterClass;
	private final Function<Filter, FilterElement> factory;

	private FilterTranslation(Class<? extends Filter> filterClass, Function<Filter, FilterElement> factory) {
		this.filterClass = filterClass;
		this.factory = factory;
	}

	public static FilterElement translate(Filter filter) {
		for (FilterTranslation trans : FilterTranslation.values()) {
			if (filter.getClass().equals(trans.filterClass)) {
				return trans.factory.apply(filter);
			}
		}
		throw new IllegalArgumentException("No translation found for filter type: " + filter.getClass());
	}
}
