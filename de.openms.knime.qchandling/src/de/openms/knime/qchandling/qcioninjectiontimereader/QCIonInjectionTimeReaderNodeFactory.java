package de.openms.knime.qchandling.qcioninjectiontimereader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "QCIonInjectionTimeReader" Node. Reads an
 * ion injection time summary file into a KNIME table.
 * 
 * @author Stephan Aiche and the OpenMS Team
 */
public class QCIonInjectionTimeReaderNodeFactory extends
        NodeFactory<QCIonInjectionTimeReaderNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public QCIonInjectionTimeReaderNodeModel createNodeModel() {
        return new QCIonInjectionTimeReaderNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<QCIonInjectionTimeReaderNodeModel> createNodeView(
            final int viewIndex,
            final QCIonInjectionTimeReaderNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

}
