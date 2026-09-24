package org.sagebionetworks.docusign;

import java.util.List;

import org.apache.commons.lang3.Strings;

import com.docusign.esign.model.DateSigned;
import com.docusign.esign.model.EmailAddress;
import com.docusign.esign.model.FullName;
import com.docusign.esign.model.PrefillTabs;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

enum TabType {
	TEXT {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			Text text = new Text();
			text.setTabLabel(label);
			text.setValue(value);
			tabs.addTextTabsItem(text);
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getTextTabs() == null) {
				return false;
			}
			for (Text t : tabs.getTextTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			if (tabs.getTextTabs() != null) {
				for (Text t : tabs.getTextTabs()) {
					if (Strings.CS.equals(label, t.getTabLabel())) {
						t.setValue(value);
						return;
					}
				}
			}
			throw new IllegalArgumentException(noSuchTabMessage(this, label));
		}
	},
	FULL_NAME {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			FullName fn = new FullName();
			fn.setTabLabel(label);
			fn.setValue(value);
			tabs.addFullNameTabsItem(fn);
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getFullNameTabs() == null) {
				return false;
			}
			for (FullName t : tabs.getFullNameTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			if (tabs.getFullNameTabs() != null) {
				for (FullName t : tabs.getFullNameTabs()) {
					if (Strings.CS.equals(label, t.getTabLabel())) {
						t.setValue(value);
						return;
					}
				}
			}
			throw new IllegalArgumentException(noSuchTabMessage(this, label));
		}
	},
	EMAIL_ADDRESS {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			EmailAddress e = new EmailAddress();
			e.setTabLabel(label);
			e.setValue(value);
			tabs.addEmailAddressTabsItem(e);
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getEmailAddressTabs() == null) {
				return false;
			}
			for (EmailAddress t : tabs.getEmailAddressTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			if (tabs.getEmailAddressTabs() != null) {
				for (EmailAddress t : tabs.getEmailAddressTabs()) {
					if (Strings.CS.equals(label, t.getTabLabel())) {
						t.setValue(value);
						return;
					}
				}
			}
			throw new IllegalArgumentException(noSuchTabMessage(this, label));
		}
	},
	/**
	 * A "sender field": a text tab owned by the document rather than by any recipient, whose value only
	 * the sender can set. Having no recipient, it has no template role for DocuSign to resolve its
	 * placement from, so it cannot be created from a label alone the way the other types can.
	 */
	PREFILL_TEXT {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("A PREFILL_TEXT tab has no recipient to resolve its"
					+ " placement, so it has to be copied from the template rather than created from a label.");
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			for (Text t : prefillTextTabs(tabs)) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			for (Text t : prefillTextTabs(tabs)) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					t.setValue(value);
					return;
				}
			}
			throw new IllegalArgumentException(noSuchTabMessage(this, label));
		}
	},
	SIGN_HERE {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("A SIGN_HERE tab does not carry a value.");
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getSignHereTabs() == null) {
				return false;
			}
			for (SignHere t : tabs.getSignHereTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;

		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("A SIGN_HERE tab does not carry a value.");
		}
	},
	DATE_SIGNED {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("A DATE_SIGNED tab does not carry a value.");
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getDateSignedTabs() == null) {
				return false;
			}
			for (DateSigned t : tabs.getDateSignedTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;

		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("A DATE_SIGNED tab does not carry a value.");
		}
	};

	/**
	 * Adds a new tab of this type carrying the given label and value. The tab has no placement, so
	 * it is only meaningful where DocuSign resolves the placement from a template by matching the
	 * label, as it does for the roles of an envelope being created from a template.
	 */
	public abstract void addTabWithLabel(Tabs tabs, String label, String value);

	public abstract boolean hasTabWithLabel(Tabs tabs, String label);

	/**
	 * Sets the value of the existing tab of this type carrying the given label, leaving the rest of
	 * its definition — including its placement — untouched.
	 *
	 * @throws IllegalArgumentException if there is no tab of this type with that label
	 * @throws UnsupportedOperationException if tabs of this type do not carry a value
	 */
	public abstract void applyValueToTabWithLabel(Tabs tabs, String label, String value);

	private static String noSuchTabMessage(TabType type, String label) {
		return "There is no " + type.name() + " tab labeled '" + label + "'.";
	}

	// Prefill tabs sit one level deeper than every other type: Tabs holds a single PrefillTabs
	// container rather than a list, and either it or its text tabs may be absent.
	private static List<Text> prefillTextTabs(Tabs tabs) {
		PrefillTabs prefillTabs = tabs.getPrefillTabs();
		if (prefillTabs == null || prefillTabs.getTextTabs() == null) {
			return List.of();
		}
		return prefillTabs.getTextTabs();
	}
}


