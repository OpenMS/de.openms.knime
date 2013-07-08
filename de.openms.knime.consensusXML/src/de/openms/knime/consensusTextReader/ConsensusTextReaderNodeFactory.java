package de.openms.knime.consensusTextReader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ConsensusTextReader" Node.
 * 
 * 
 * @author Stephan Aiche and the OpenMS Team
 */
public class ConsensusTextReaderNodeFactory extends
		NodeFactory<ConsensusTextReaderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConsensusTextReaderNodeModel createNodeModel() {
		return new ConsensusTextReaderNodeModel();
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
	public NodeView<ConsensusTextReaderNodeModel> createNodeView(
			final int viewIndex, final ConsensusTextReaderNodeModel nodeModel) {
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
