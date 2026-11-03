package org.sagebionetworks.docusign;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.docusign.esign.model.Tabs;

/**
 * Rewrites the identifiers of every tab in a {@link Tabs}, whatever its type.
 * <p>
 * A tab definition read from a template carries the tab and recipient IDs it had there, neither of
 * which is valid in the envelope it is being reused in. Template authors are free to use tab types
 * this code knows nothing about — their signers fill those in and the values are captured in the
 * finished document — so every tab has to be corrected, not just the {@link TabType}s whose values
 * this code supplies.
 */
class TabIdentifiers {

	/**
	 * Points every tab at the given recipient and clears its tab ID, which DocuSign assigns itself.
	 *
	 * @param tabs mutated in place
	 */
	static void assignToRecipient(Tabs tabs, String recipientId) {
		for (List<?> tabsOfOneType : tabLists(tabs)) {
			for (Object tab : tabsOfOneType) {
				// Tab types are unrelated classes with no common supertype, and not all of them
				// carry both identifiers — a RadioGroup, for one, has no tab ID. A type that does
				// not declare a setter simply has nothing to correct.
				invokeIfPresent(tab, "setTabId", null);
				invokeIfPresent(tab, "setRecipientId", recipientId);
			}
		}
	}

	// Every getXxxTabs() accessor that Tabs declares as a list, so that a tab type added to a later
	// release of the DocuSign SDK is covered without this class having to change. Prefill tabs are
	// excluded by the same rule: Tabs exposes them as a single container, and being filled in by the
	// sender rather than by a recipient they have no recipient to be pointed at.
	private static List<List<?>> tabLists(Tabs tabs) {
		List<List<?>> tabLists = new ArrayList<>();
		for (Method method : Tabs.class.getMethods()) {
			if (method.getParameterCount() != 0
					|| !method.getName().startsWith("get")
					|| !method.getName().endsWith("Tabs")
					|| !List.class.isAssignableFrom(method.getReturnType())) {
				continue;
			}
			List<?> tabsOfOneType = (List<?>) invoke(method, tabs);
			if (tabsOfOneType != null) {
				tabLists.add(tabsOfOneType);
			}
		}
		return tabLists;
	}

	private static void invokeIfPresent(Object tab, String setterName, String value) {
		Method setter;
		try {
			setter = tab.getClass().getMethod(setterName, String.class);
		} catch (NoSuchMethodException e) {
			return;
		}
		invoke(setter, tab, value);
	}

	private static Object invoke(Method method, Object target, Object... arguments) {
		try {
			return method.invoke(target, arguments);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(
					"Failed to call " + method.getName() + " on " + target.getClass().getSimpleName(), e);
		}
	}
}
