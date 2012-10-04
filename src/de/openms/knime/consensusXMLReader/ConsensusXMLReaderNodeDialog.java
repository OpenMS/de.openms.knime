package de.openms.knime.consensusXMLReader;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * <code>NodeDialog</code> for the "ConsensusXMLReader" Node.
 * Converts consesnsusXML files to KNIME tables.	
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
     * New pane for configuring ConsensusXMLReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ConsensusXMLReaderNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    ConsensusXMLReaderNodeModel.CFGKEY_COUNT,
                    ConsensusXMLReaderNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}

