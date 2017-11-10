/**
 * Copyright (c) 2013, Stephan Aiche, Freie Universitaet Berlin
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of the Freie Universitaet Berlin nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.openms.knime.qchandling.qcprecursorreader;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
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
 * This is the model implementation of QCPrecursorReader. Read the precursor
 * file created by the QCCalculator
 * 
 * @author Stephan Aiche
 */
public class QCPrecursorReaderNodeModel extends NodeModel {

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
    protected QCPrecursorReaderNodeModel() {
        super(getIncomingPorts(), getOutgoingPorts());
    }

    private DataTableSpec createColumnSpec() {
        // RT MZ uniqueness ProteinID target/decoy Score PeptideSequence Annots
        // Similarity Charge TheoreticalWeight Oxidation (M)

        DataColumnSpec[] allColSpecs = new DataColumnSpec[5];
        allColSpecs[0] = new DataColumnSpecCreator("RT", DoubleCell.TYPE)
                .createSpec();
        allColSpecs[1] = new DataColumnSpecCreator("Precursor", DoubleCell.TYPE)
                .createSpec();
        allColSpecs[2] = new DataColumnSpecCreator("Charge", IntCell.TYPE)
                .createSpec();
        allColSpecs[3] = new DataColumnSpecCreator("S/N", DoubleCell.TYPE)
                .createSpec();
        allColSpecs[4] = new DataColumnSpecCreator("Peak Count", IntCell.TYPE)
                .createSpec();

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
        TSVReader precursorTSVReader = new TSVReader(5) {

            @Override
            protected DataCell[] parseLine(String[] tokens) {
                DataCell[] cells = new DataCell[5];

                cells[0] = new DoubleCell(Double.parseDouble(tokens[0]));
                cells[1] = new DoubleCell(Double.parseDouble(tokens[1]));
                cells[2] = new IntCell(Integer.parseInt(tokens[2]));
                cells[3] = new DoubleCell(Double.parseDouble(tokens[3]));
                cells[4] = new IntCell(Integer.parseInt(tokens[4]));

                return cells;
            }

            @Override
            protected String[] getHeader() {
                return new String[] { "MS:1000894_[sec]", "MS:1000040", "MS:1000041", "S/N", "peak_count" };
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
