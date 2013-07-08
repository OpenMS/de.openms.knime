package de.openms.knime.mztab.small_molecule;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SmallMoleculeMzTabReader" Node. This node
 * reads the small molecule section of the mzTab standard (as of 07/2013).
 * 
 * @author Stephan Aiche on behalf of the OpenMS Team
 */
public class SmallMoleculeMzTabReaderNodeFactory extends
		NodeFactory<SmallMoleculeMzTabReaderNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SmallMoleculeMzTabReaderNodeModel createNodeModel() {
		return new SmallMoleculeMzTabReaderNodeModel();
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
	public NodeView<SmallMoleculeMzTabReaderNodeModel> createNodeView(
			final int viewIndex,
			final SmallMoleculeMzTabReaderNodeModel nodeModel) {
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
