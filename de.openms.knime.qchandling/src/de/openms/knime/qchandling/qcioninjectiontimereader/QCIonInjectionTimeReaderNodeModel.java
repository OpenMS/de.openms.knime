package de.openms.knime.qchandling.qcioninjectiontimereader;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.IURIPortObject;
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

import de.openms.knime.qchandling.TSVReader;
import de.openms.knime.qchandling.TSVReader.InvalidHeaderException;
import de.openms.knime.qchandling.TSVReader.InvalidLineException;

/**
 * This is the model implementation of QCIonInjectionTimeReader. Reads an ion
 * injection time summary file into a KNIME table.
 * 
 * @author Stephan Aiche and the OpenMS Team
 */
public class QCIonInjectionTimeReaderNodeModel extends NodeModel {

    // the logger instance
    @SuppressWarnings("unused")
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(QCIonInjectionTimeReaderNodeModel.class);

    /**
     * Static method that provides the incoming {@link PortType}s.
     * 
     * @return The incoming {@link PortType}s of this node.
     */
    private static PortType[] getIncomingPorts() {
        return new PortType[] { IURIPortObject.TYPE };
    }

    /**
     * Static method that provides the outgoing {@link PortType}s.
     * 
     * @return The outgoing {@link PortType}s of this node.
     */
    private static PortType[] getOutgoingPorts() {
        return new PortType[] { new PortType(BufferedDataTable.class) };
    }

    /**
     * Constructor for the node model.
     */
    protected QCIonInjectionTimeReaderNodeModel() {
        super(getIncomingPorts(), getOutgoingPorts());
    }

    private DataTableSpec createColumnSpec() {
        // RT MZ uniqueness ProteinID target/decoy Score PeptideSequence Annots
        // Similarity Charge TheoreticalWeight Oxidation (M)

        DataColumnSpec[] allColSpecs = new DataColumnSpec[2];
        allColSpecs[0] = new DataColumnSpecCreator("RunId", StringCell.TYPE)
                .createSpec();
        allColSpecs[1] = new DataColumnSpecCreator("IonInjectionTimeAverage",
                DoubleCell.TYPE).createSpec();

        return new DataTableSpec(allColSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws IOException,
            InvalidLineException, CanceledExecutionException,
            InvalidHeaderException {
        TSVReader precursorTSVReader = new TSVReader(2, true) {

            @Override
            protected DataCell[] parseLine(String[] tokens) {
                DataCell[] cells = new DataCell[2];

                cells[0] = new StringCell(tokens[0]);
                cells[1] = new DoubleCell(Double.parseDouble(tokens[1]));

                return cells;
            }

            @Override
            protected String[] getHeader() {
                return new String[] { "run_id", "ion_inj_time_ms1_avg" };
            }
        };

        BufferedDataContainer container = exec
                .createDataContainer(createColumnSpec());
        precursorTSVReader.run(new File(((IURIPortObject) inData[0])
                .getURIContents().get(0).getURI()), container, exec);

        container.close();
        return new BufferedDataTable[] { container.getTable() };
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
        return new DataTableSpec[] { createColumnSpec() };
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
