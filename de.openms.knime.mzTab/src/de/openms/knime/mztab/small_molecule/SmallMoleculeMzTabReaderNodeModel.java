/**
 * --------------------------------------------------------------------------
 *                   OpenMS -- Open-Source Mass Spectrometry
 * --------------------------------------------------------------------------
 * Copyright The OpenMS Team -- Eberhard Karls University Tuebingen,
 * ETH Zurich, and Freie Universitaet Berlin 2002-2013.
 * 
 * This software is released under a three-clause BSD license:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of any author or any participating institution
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * For a full list of authors, refer to the file AUTHORS.
 * --------------------------------------------------------------------------
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL ANY OF THE AUTHORS OR THE CONTRIBUTING
 * INSTITUTIONS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.openms.knime.mztab.small_molecule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.URIContent;
import org.knime.core.data.uri.URIPortObject;
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

import de.openms.knime.InvalidInputException;
import de.openms.knime.mztab.small_molecule.exceptions.InvalidMzTabFormatException;
import de.openms.knime.mztab.small_molecule.exceptions.MissingSmallMoleculeContentException;

/**
 * This is the model implementation of SmallMoleculeMzTabReader. This node reads
 * the small molecule section of the mzTab standard (as of 07/2013).
 * 
 * @author Stephan Aiche on behalf of the OpenMS Team
 */
public class SmallMoleculeMzTabReaderNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SmallMoleculeMzTabReaderNodeModel.class);

    /**
     * Constructor for the node model.
     */
    protected SmallMoleculeMzTabReaderNodeModel() {
        super(new PortType[] { new PortType(URIPortObject.class) },
                new PortType[] { new PortType(BufferedDataTable.class) });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws InvalidMzTabFormatException,
            MissingSmallMoleculeContentException, InvalidInputException,
            IOException {

        URIPortObject obj = (URIPortObject) inObjects[0];
        List<URIContent> uris = obj.getURIContents();
        if (uris.size() == 0) {
            throw new InvalidInputException(
                    "No URI was supplied in URIPortObject at input port 0");
        } else if (uris.size() != 1) {
            throw new InvalidInputException(String.format(
                    "We can only demangle a single file but got %d.",
                    uris.size()));
        }

        URI relURI = uris.get(0).getURI();
        File cXMLFile = new File(relURI);

        BufferedReader brReader = null;
        BufferedDataContainer container = exec
                .createDataContainer(createMzTabSmallMoleculeSpec());
        BufferedDataTable out = null;
        try {
            // read the data and fill the table
            brReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(cXMLFile)));

            // validate mzTab starting point
            String line = brReader.readLine();
            if (!line.trim().startsWith("MTD")) {
                throw new InvalidMzTabFormatException(
                        "Invalid start of file: mzTab file should start with the line: 'MTD\tmzTab-version\t1.0.0'");
            }

            // find SMH -> Small Molecule Header
            while ((line = brReader.readLine()) != null) {
                if (line.trim().startsWith("SMH")) {
                    // check if the header is valid
                    validateSMHLine(container, line);
                    // we have a valid header
                    break;
                }
            }

            if (line == null) {
                throw new MissingSmallMoleculeContentException(
                        "Invalid mzTab file: The file does not contain a small molecule header (SMH)");
            }

            int rowIdx = 1;

            do {
                // we only parse SML entries
                if (line.trim().startsWith("SML")) {
                    DataCell[] cells = parseSMLLine(container, line);
                    RowKey key = new RowKey("Row " + rowIdx++);
                    DataRow row = new DefaultRow(key, cells);
                    container.addRowToTable(row);
                    exec.checkCanceled();
                }
            } while ((line = brReader.readLine()) != null);

            // close container/table
            container.close();
            out = container.getTable();
        } catch (CanceledExecutionException e) {
            logger.info("Canceled execution!");
        } finally {
            if (brReader != null)
                brReader.close();
        }

        return new BufferedDataTable[] { out };
    }

    public DataCell[] parseSMLLine(BufferedDataContainer container, String line)
            throws InvalidMzTabFormatException {
        String[] line_entries = line.split("\t");
        if (line_entries.length <= container.getTableSpec().getNumColumns()) {
            throw new InvalidMzTabFormatException(
                    "Invalid SML line in mzTab file.");
        }

        DataCell[] cells = new DataCell[container.getTableSpec()
                .getNumColumns()];

        // convert the entries into a table column
        for (int i = 0; i < container.getTableSpec().getNumColumns(); ++i) {
            if (container.getTableSpec().getColumnSpec(i).getType() == IntCell.TYPE) {
                cells[i] = new IntCell(Integer.parseInt(line_entries[i + 1]));
            } else if (container.getTableSpec().getColumnSpec(i).getType() == DoubleCell.TYPE) {
                // we need to make sure that it is a proper value
                if (line_entries[i + 1].equals("INF")
                        || line_entries[i + 1].equals("NaN")
                        || line_entries[i + 1].equals("null")) {
                    cells[i] = new DoubleCell(-1.0);
                } else {
                    cells[i] = new DoubleCell(
                            Double.parseDouble(line_entries[i + 1]));
                }
            } else {
                // it is a string value -> just put it into the
                // table
                cells[i] = new StringCell(line_entries[i + 1]);
            }
        }
        return cells;
    }

    public void validateSMHLine(BufferedDataContainer container, String line)
            throws InvalidMzTabFormatException {
        // validate SMH header against our mzTab version
        String[] header_entries = line.split("\t");
        if (header_entries.length <= container.getTableSpec().getNumColumns()) {
            throw new InvalidMzTabFormatException(
                    "Invalid mzTab small molecule header (SMH). The header has not enough entries.");
        }
        // compare actual header against expected header entries
        for (int i = 0; i < container.getTableSpec().getNumColumns(); ++i) {
            if (!header_entries[i + 1].equals(container.getTableSpec()
                    .getColumnSpec(i).getName())) {
                throw new InvalidMzTabFormatException(
                        String.format(
                                "Invalid entry in small molecule header: Expected '%s' but got '%s'",
                                container.getTableSpec().getColumnSpec(i)
                                        .getName(), header_entries[i + 1]));
            }
        }
        if (header_entries.length != container.getTableSpec().getNumColumns() + 1) {
            setWarningMessage("mzTab file seems to contain optional columns. These will not be contained in the table.");
        }
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
        return new DataTableSpec[] { createMzTabSmallMoleculeSpec(), null };
    }

    private DataTableSpec createMzTabSmallMoleculeSpec() {
        // MTD mzTab-version 1.0.0
        // SMH identifier unit_id chemical_formula smiles inchi_key description
        // mass_to_charge charge retention_time taxid species database
        // database_version reliability uri spectra_ref search_engine
        // search_engine_score modifications smallmolecule_abundance_sub[1]
        // smallmolecule_abundance_stdev_sub[1]
        // smallmolecule_abundance_std_error_sub[1]

        DataColumnSpec[] specs = new DataColumnSpec[22];
        specs[0] = new DataColumnSpecCreator("identifier", StringCell.TYPE)
                .createSpec();
        specs[1] = new DataColumnSpecCreator("unit_id", StringCell.TYPE)
                .createSpec();
        specs[2] = new DataColumnSpecCreator("chemical_formula",
                StringCell.TYPE).createSpec();
        specs[3] = new DataColumnSpecCreator("smiles", StringCell.TYPE)
                .createSpec();
        specs[4] = new DataColumnSpecCreator("inchi_key", StringCell.TYPE)
                .createSpec();
        specs[5] = new DataColumnSpecCreator("description", StringCell.TYPE)
                .createSpec();
        specs[6] = new DataColumnSpecCreator("mass_to_charge", DoubleCell.TYPE)
                .createSpec();
        specs[7] = new DataColumnSpecCreator("charge", IntCell.TYPE)
                .createSpec();
        specs[8] = new DataColumnSpecCreator("retention_time", DoubleCell.TYPE)
                .createSpec();
        specs[9] = new DataColumnSpecCreator("taxid", StringCell.TYPE)
                .createSpec();
        specs[10] = new DataColumnSpecCreator("species", StringCell.TYPE)
                .createSpec();
        specs[11] = new DataColumnSpecCreator("database", StringCell.TYPE)
                .createSpec();
        specs[12] = new DataColumnSpecCreator("database_version",
                StringCell.TYPE).createSpec();
        specs[13] = new DataColumnSpecCreator("reliability", StringCell.TYPE)
                .createSpec();
        specs[14] = new DataColumnSpecCreator("uri", StringCell.TYPE)
                .createSpec();
        specs[15] = new DataColumnSpecCreator("spectra_ref", StringCell.TYPE)
                .createSpec();
        specs[16] = new DataColumnSpecCreator("search_engine", StringCell.TYPE)
                .createSpec();
        specs[17] = new DataColumnSpecCreator("search_engine_score",
                StringCell.TYPE).createSpec();
        specs[18] = new DataColumnSpecCreator("modifications", StringCell.TYPE)
                .createSpec();
        specs[19] = new DataColumnSpecCreator("smallmolecule_abundance_sub[1]",
                DoubleCell.TYPE).createSpec();
        specs[20] = new DataColumnSpecCreator(
                "smallmolecule_abundance_stdev_sub[1]", DoubleCell.TYPE)
                .createSpec();
        specs[21] = new DataColumnSpecCreator(
                "smallmolecule_abundance_std_error_sub[1]", DoubleCell.TYPE)
                .createSpec();

        return new DataTableSpec(specs);
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
