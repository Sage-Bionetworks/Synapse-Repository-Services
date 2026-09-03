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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getTextTabs() != null) {
				for (Text t : tabs.getTextTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getFullNameTabs() != null) {
				for (FullName t : tabs.getFullNameTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getTitleTabs() != null) {
				for (Title t : tabs.getTitleTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getEmailAddressTabs() != null) {
				for (EmailAddress t : tabs.getEmailAddressTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
				}
			}
			if (tabs.getEmailTabs() != null) {
				for (Email t : tabs.getEmailTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getSignHereTabs() != null) {
				for (SignHere t : tabs.getSignHereTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
		public void assignTabsToRecipient(Tabs tabs, String recipientId) {
			if (tabs.getDateSignedTabs() != null) {
				for (DateSigned t : tabs.getDateSignedTabs()) {
					t.setTabId(null);
					t.setRecipientId(recipientId);
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
	 * Points every tab of this type at the given recipient and clears its tab ID. A tab definition
	 * read from a template carries the tab and recipient IDs it had there, neither of which is valid
	 * in the envelope it is being reused in, so the recipient is restated and the tab ID is left for
	 * DocuSign to assign.
	 */
	public abstract void assignTabsToRecipient(Tabs tabs, String recipientId);

	private static String noSuchTabMessage(TabType type, String label) {
		return "There is no " + type.name() + " tab labeled '" + label + "'.";
	}
}


