package de.openms.knime.consensusXMLReader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
import org.knime.core.data.uri.URIPortObjectSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import de.openms.consensusXML.ConsensusElement;
import de.openms.consensusXML.ConsensusXML;
import de.openms.consensusXML.Element;

/**
 * This is the model implementation of ConsensusXMLReader. Converts
 * consesnsusXML files to KNIME tables.
 * 
 * @author The OpenMS Team
 */
public class ConsensusXMLReaderNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(ConsensusXMLReaderNodeModel.class);

	/**
	 * Constructor for the node model.
	 */
	protected ConsensusXMLReaderNodeModel() {
		super(new PortType[] { new PortType(URIPortObject.class) },
				new PortType[] { new PortType(BufferedDataTable.class) });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {
		logger.info("Node Model Stub... this is not yet implemented !");

		URIPortObject obj = (URIPortObject) inObjects[0];
		List<URIContent> uris = obj.getURIContents();
		if (uris.size() == 0) {
			throw new Exception(
					"No URI was supplied in MIMEURIPortObject at input port 0");
		} else if (uris.size() != 1) {
			throw new Exception(String.format(
					"We can only demangle a single file but got %d.",
					uris.size()));
		}

		URI relURI = uris.get(0).getURI();
		File cXMLFile = new File(relURI);

		JAXBContext jc = JAXBContext.newInstance("de.openms.consensusXML");
		Unmarshaller u = jc.createUnmarshaller();
		ConsensusXML cXMLDocument = (ConsensusXML) u.unmarshal(cXMLFile);

		// create the container based on the data set
		BufferedDataContainer container = exec
				.createDataContainer(getFeatureTableSpec(cXMLDocument));

		int mapCount = cXMLDocument.getMapList().getMap().size();
		int colCount = 6 + (mapCount * 3);
		int rowIdx = 1;

		for (ConsensusElement cElem : cXMLDocument.getConsensusElementList()
				.getConsensusElement()) {

			DataCell[] rowCells = new DataCell[colCount];
			rowCells[0] = new StringCell(cElem.getId());
			rowCells[1] = new IntCell((cElem.getCharge() != null ? cElem
					.getCharge().intValue() : -1));
			rowCells[2] = new DoubleCell(cElem.getQuality());
			rowCells[3] = new DoubleCell(cElem.getCentroid().getRt());
			rowCells[4] = new DoubleCell(cElem.getCentroid().getMz());
			rowCells[5] = new DoubleCell(cElem.getCentroid().getIt());

			// fill cells
			for (int i = 0; i < cXMLDocument.getMapList().getMap().size(); ++i) {
				rowCells[(6 + (i * 3))] = new DoubleCell(-1);
				rowCells[(7 + (i * 3))] = new DoubleCell(-1);
				rowCells[(8 + (i * 3))] = new DoubleCell(-1);
			}

			// add real values
			for (Element elem : cElem.getGroupedElementList().getElement()) {
				rowCells[(int) (6 + (elem.getMap() * 3))] = new DoubleCell(
						elem.getRt());
				rowCells[(int) (7 + (elem.getMap() * 3))] = new DoubleCell(
						elem.getMz());
				rowCells[(int) (8 + (elem.getMap() * 3))] = new DoubleCell(
						elem.getIt());
			}

			container.addRowToTable(new DefaultRow(String.format("Row %d",
					rowIdx++), rowCells));
		}

		container.close();
		BufferedDataTable out = container.getTable();

		return new BufferedDataTable[] { out };
	}

	private DataTableSpec getFeatureTableSpec(ConsensusXML cXMLDocument) {

		DataColumnSpec[] allColSpecs = new DataColumnSpec[6 + (cXMLDocument
				.getMapList().getMap().size() * 3)];

		// the general values
		allColSpecs[0] = new DataColumnSpecCreator("ID", StringCell.TYPE)
				.createSpec();
		allColSpecs[1] = new DataColumnSpecCreator("charge", IntCell.TYPE)
				.createSpec();
		allColSpecs[2] = new DataColumnSpecCreator("quality", DoubleCell.TYPE)
				.createSpec();
		allColSpecs[3] = new DataColumnSpecCreator("centroid_rt",
				DoubleCell.TYPE).createSpec();
		allColSpecs[4] = new DataColumnSpecCreator("centroid_mz",
				DoubleCell.TYPE).createSpec();
		allColSpecs[5] = new DataColumnSpecCreator("centroid_intensity",
				DoubleCell.TYPE).createSpec();

		for (int i = 0; i < cXMLDocument.getMapList().getMap().size(); ++i) {
			allColSpecs[6 + (i * 3)] = new DataColumnSpecCreator(String.format(
					"map_%d_rt", i), DoubleCell.TYPE).createSpec();
			allColSpecs[7 + (i * 3)] = new DataColumnSpecCreator(String.format(
					"map_%d_mz", i), DoubleCell.TYPE).createSpec();
			allColSpecs[8 + (i * 3)] = new DataColumnSpecCreator(String.format(
					"map_%d_intensity", i), DoubleCell.TYPE).createSpec();
		}

		return new DataTableSpec(allColSpecs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
			throws InvalidSettingsException {
		// we do not know how the table looks before we read the complete
		// consensusXML file

		if (!(inSpecs[0] instanceof URIPortObjectSpec)) {
			throw new InvalidSettingsException(
					"no MIMEURIPortObject compatible port object at port 0");
		}

		URIPortObjectSpec spec = (URIPortObjectSpec) inSpecs[0];
		if (!"consensusxml".equals(spec.getFileExtensions().get(0)
				.toLowerCase())) {
			throw new InvalidSettingsException(
					"This node can only parse consensusXML files but got "
							+ spec.getFileExtensions().get(0) + ".");
		}

		return new DataTableSpec[] { null };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
	}

}
