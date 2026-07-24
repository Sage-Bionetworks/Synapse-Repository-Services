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
		public void fillInTabValue(Tabs tabs, String label, String value) {
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
	},
	FULL_NAME {
		@Override
		public void fillInTabValue(Tabs tabs, String label, String value) {
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
	},		
	TITLE {
		@Override
		public void fillInTabValue(Tabs tabs, String label, String value) {
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
	},
	EMAIL_ADDRESS {
		@Override
		public void fillInTabValue(Tabs tabs, String label, String value) {
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
	},
	SIGN_HERE {
		@Override
		public void fillInTabValue(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("Cannot set the value of a SIGN_HERE tab.");
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
	},
	DATE_SIGNED {
		@Override
		public void fillInTabValue(Tabs tabs, String label, String value) {
			throw new UnsupportedOperationException("Cannot set the value of a DATE_SIGNED tab.");
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
	};
	
	public abstract void fillInTabValue(Tabs tabs, String label, String value);
	public abstract boolean hasTabWithLabel(Tabs tabs, String label);
}


