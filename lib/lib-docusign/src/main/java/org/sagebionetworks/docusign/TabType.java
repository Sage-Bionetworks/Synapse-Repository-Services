package org.sagebionetworks.docusign;

import org.apache.commons.lang3.Strings;

import com.docusign.esign.model.DateSigned;
import com.docusign.esign.model.Email;
import com.docusign.esign.model.EmailAddress;
import com.docusign.esign.model.FullName;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;
import com.docusign.esign.model.Title;

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
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getTextTabs() != null) {
				for (Text t : tabs.getTextTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
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
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getFullNameTabs() != null) {
				for (FullName t : tabs.getFullNameTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
		}
	},
	TITLE {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			Title t = new Title();
			t.setTabLabel(label);
			t.setValue(value);
			tabs.addTitleTabsItem(t);
		}
		@Override
		public boolean hasTabWithLabel(Tabs tabs, String label) {
			if (tabs.getTitleTabs() == null) {
				return false;
			}
			for (Title t : tabs.getTitleTabs()) {
				if (Strings.CS.equals(label, t.getTabLabel())) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void applyValueToTabWithLabel(Tabs tabs, String label, String value) {
			if (tabs.getTitleTabs() != null) {
				for (Title t : tabs.getTitleTabs()) {
					if (Strings.CS.equals(label, t.getTabLabel())) {
						t.setValue(value);
						return;
					}
				}
			}
			throw new IllegalArgumentException(noSuchTabMessage(this, label));
		}
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getTitleTabs() != null) {
				for (Title t : tabs.getTitleTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
		}
	},
	EMAIL_ADDRESS {
		@Override
		public void addTabWithLabel(Tabs tabs, String label, String value) {
			Email e = new Email();
			e.setTabLabel(label);
			e.setValue(value);
			tabs.addEmailTabsItem(e);
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
		// A template declares its email tabs as emailAddressTabs, which is where an existing
		// definition is found and updated. Note that addTabWithLabel above instead adds an entry to
		// emailTabs, which DocuSign treats as a distinct tab type.
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
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getEmailAddressTabs() != null) {
				for (EmailAddress t : tabs.getEmailAddressTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
			if (tabs.getEmailTabs() != null) {
				for (Email t : tabs.getEmailTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
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
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getSignHereTabs() != null) {
				for (SignHere t : tabs.getSignHereTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
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
		@Override
		public void clearTabIdentifiers(Tabs tabs) {
			if (tabs.getDateSignedTabs() != null) {
				for (DateSigned t : tabs.getDateSignedTabs()) {
					t.setTabId(null);
					t.setRecipientId(null);
				}
			}
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

	/**
	 * Clears the tab and recipient IDs of every tab of this type. Both identify a tab within the
	 * envelope or template it was read from, so they must not be carried over when the definition is
	 * reused elsewhere; DocuSign assigns new ones.
	 */
	public abstract void clearTabIdentifiers(Tabs tabs);

	private static String noSuchTabMessage(TabType type, String label) {
		return "There is no " + type.name() + " tab labeled '" + label + "'.";
	}
}


