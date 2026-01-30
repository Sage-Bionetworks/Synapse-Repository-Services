package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.select;

import java.util.function.Function;

import org.sagebionetworks.repo.model.grid.query.SelectAll;
import org.sagebionetworks.repo.model.grid.query.SelectByName;
import org.sagebionetworks.repo.model.grid.query.SelectItem;
import org.sagebionetworks.repo.model.grid.query.SelectSelection;
import org.sagebionetworks.repo.model.grid.query.function.CountStar;

public enum SelectItemTranslator {
	CountStar(CountStar.class, CountStartElement::new),
	SelectAll(SelectAll.class, SelectAllElement::new),
	SelectByName(SelectByName.class, SelectByNameElement::new),
	SelectSelection(SelectSelection.class, SelectSelectionElement::new);

	private final Class<? extends SelectItem> itemClass;
	private final Function<SelectItem, SelectItemElement> factory;

	private SelectItemTranslator(Class<? extends SelectItem> itemClass, Function<SelectItem, SelectItemElement> factory) {
		this.itemClass = itemClass;
		this.factory = factory;
	}

	public static SelectItemElement translate(SelectItem item) {
		for (SelectItemTranslator trans : SelectItemTranslator.values()) {
			if (item.getClass().equals(trans.itemClass)) {
				return trans.factory.apply(item);
			}
		}
		throw new IllegalArgumentException("No translation found for select item type: " + item.getClass());
	}

}
