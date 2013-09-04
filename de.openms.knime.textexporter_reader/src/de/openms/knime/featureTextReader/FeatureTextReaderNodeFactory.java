package de.openms.knime.featureTextReader;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "FeatureTextReader" Node. Reads files
 * exported b...
 * 
 * @author The OpenMS Team
 */
public class FeatureTextReaderNodeFactory extends
		NodeFactory<FeatureTextReaderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FeatureTextReaderNodeModel createNodeModel() {
		return new FeatureTextReaderNodeModel();
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
	public NodeView<FeatureTextReaderNodeModel> createNodeView(
			final int viewIndex, final FeatureTextReaderNodeModel nodeModel) {
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
