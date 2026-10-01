package org.sagebionetworks.docusign;

import java.util.List;
import java.util.Map;

import com.docusign.esign.model.Text;

/**
 * Where a validated eDUC template keeps each of the values Synapse fills in.
 * <p>
 * A field may be given any of several DocuSign tab types, and the one a template chose decides both
 * how its value is written and whether it belongs to a signer or to the document. Resolving that once,
 * while the template is being validated, is what keeps the code that fills a template in from having
 * to guess: DocuSign matches a supplied tab to the template by type as well as by label, so a guess
 * that disagrees with the template is not an error it reports but a value it silently drops.
 */
class EDucTemplateLayout {

	private final Map<RoleLabelKey, TabType> typesByRoleAndLabel;
	private final List<Text> senderFieldDefinitions;

	EDucTemplateLayout(Map<RoleLabelKey, TabType> typesByRoleAndLabel, List<Text> senderFieldDefinitions) {
		this.typesByRoleAndLabel = typesByRoleAndLabel;
		this.senderFieldDefinitions = senderFieldDefinitions;
	}

	/**
	 * The tab type this template gives the named field.
	 *
	 * @throws IllegalArgumentException if the field is not one the template was validated to carry
	 */
	TabType typeOf(String roleName, String tabLabel) {
		TabType type = typesByRoleAndLabel.get(new RoleLabelKey(roleName, tabLabel));
		if (type == null) {
			throw new IllegalArgumentException(
					"The template has no tab labeled '" + tabLabel + "' for role '" + roleName + "'.");
		}
		return type;
	}

	/**
	 * Whether the named field is a sender field, and so belongs to the document rather than to the
	 * signer holding the role.
	 */
	boolean isSenderField(String roleName, String tabLabel) {
		return TabType.PREFILL_TEXT == typeOf(roleName, tabLabel);
	}

	/**
	 * The template's sender field definitions, gathered from all of its documents. Each carries the
	 * document ID and placement it was authored with, which is what tells an envelope's sender fields
	 * which document they belong to, and what they have to be created from if the envelope did not
	 * inherit them from the template.
	 */
	List<Text> senderFieldDefinitions() {
		return senderFieldDefinitions;
	}
}
