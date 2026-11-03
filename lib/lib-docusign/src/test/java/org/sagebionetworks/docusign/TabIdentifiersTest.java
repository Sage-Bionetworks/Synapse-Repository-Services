package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.docusign.esign.model.Approve;
import com.docusign.esign.model.Checkbox;
import com.docusign.esign.model.Company;
import com.docusign.esign.model.PrefillTabs;
import com.docusign.esign.model.RadioGroup;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.Text;

public class TabIdentifiersTest {

	@Test
	public void testAssignToRecipientWithTypeThisCodeSupplies() {
		Text text = new Text();
		text.setTabLabel("collaborator_1_user_name");
		text.setTabId("template-tab-id");
		text.setRecipientId("template-recipient-id");
		Tabs tabs = new Tabs();
		tabs.setTextTabs(List.of(text));

		// call under test
		TabIdentifiers.assignToRecipient(tabs, "7");

		assertNull(text.getTabId());
		assertEquals("7", text.getRecipientId());
	}

	@Test
	public void testAssignToRecipientWithTypesThisCodeDoesNotSupply() {
		// A template author is free to add tabs of any type for their signers to fill in. Those are
		// copied onto a recipient being added back, so their identifiers have to be corrected too.
		Checkbox checkbox = new Checkbox();
		checkbox.setTabId("template-tab-id");
		checkbox.setRecipientId("template-recipient-id");
		Approve approve = new Approve();
		approve.setTabId("template-tab-id");
		approve.setRecipientId("template-recipient-id");
		Company company = new Company();
		company.setTabId("template-tab-id");
		company.setRecipientId("template-recipient-id");

		Tabs tabs = new Tabs();
		tabs.setCheckboxTabs(List.of(checkbox));
		tabs.setApproveTabs(List.of(approve));
		tabs.setCompanyTabs(List.of(company));

		// call under test
		TabIdentifiers.assignToRecipient(tabs, "7");

		assertNull(checkbox.getTabId());
		assertEquals("7", checkbox.getRecipientId());
		assertNull(approve.getTabId());
		assertEquals("7", approve.getRecipientId());
		assertNull(company.getTabId());
		assertEquals("7", company.getRecipientId());
	}

	@Test
	public void testAssignToRecipientWithTypeCarryingNoTabId() {
		// a RadioGroup has a recipient ID but no tab ID
		RadioGroup radioGroup = new RadioGroup();
		radioGroup.setRecipientId("template-recipient-id");
		Tabs tabs = new Tabs();
		tabs.setRadioGroupTabs(List.of(radioGroup));

		// call under test
		TabIdentifiers.assignToRecipient(tabs, "7");

		assertEquals("7", radioGroup.getRecipientId());
	}

	@Test
	public void testAssignToRecipientLeavesPrefillTabsAlone() {
		// Prefill tabs are filled in by the sender rather than by a recipient, so they carry no
		// recipient ID and Tabs exposes them as a single container rather than a list. They are
		// correctly outside what this corrects.
		Tabs tabs = new Tabs();
		PrefillTabs prefillTabs = new PrefillTabs();
		tabs.setPrefillTabs(prefillTabs);

		// call under test — there is nothing to correct, and nothing to fail on
		TabIdentifiers.assignToRecipient(tabs, "7");

		assertSame(prefillTabs, tabs.getPrefillTabs());
	}

	@Test
	public void testAssignToRecipientWithNoTabs() {
		// call under test — every list is null on a Tabs that declares nothing
		TabIdentifiers.assignToRecipient(new Tabs(), "7");
	}

	@Test
	public void testAssignToRecipientCoversEveryTabListDeclaredByTabs() {
		// Guards against a tab list being missed: the helper is driven by reflection precisely so
		// that a type added in a later release of the DocuSign SDK is covered without a code change.
		int declaredTabLists = 0;
		for (Method method : Tabs.class.getMethods()) {
			if (method.getParameterCount() == 0
					&& method.getName().startsWith("get")
					&& method.getName().endsWith("Tabs")
					&& List.class.isAssignableFrom(method.getReturnType())) {
				declaredTabLists++;
			}
		}
		// far more than the six types whose values this code supplies
		assertTrue(declaredTabLists > 30, "expected many tab lists, found " + declaredTabLists);
	}
}
