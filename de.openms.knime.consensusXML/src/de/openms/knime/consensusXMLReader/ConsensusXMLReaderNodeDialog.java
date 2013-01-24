package de.openms.knime.consensusXMLReader;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "ConsensusXMLReader" Node. Converts
 * consesnsusXML files to KNIME tables.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author The OpenMS Team
 */
public class ConsensusXMLReaderNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * New pane for configuring ConsensusXMLReader node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
	protected ConsensusXMLReaderNodeDialog() {
		super();
	}
}
