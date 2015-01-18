/**
 * --------------------------------------------------------------------------
 *                   OpenMS -- Open-Source Mass Spectrometry
 * --------------------------------------------------------------------------
 * Copyright The OpenMS Team -- Eberhard Karls University Tuebingen,
 * ETH Zurich, and Freie Universitaet Berlin 2002-2015.
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
package de.openms.knime.mztab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.uri.IURIPortObject;
import org.knime.core.data.uri.URIContent;
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
import de.openms.knime.mztab.exceptions.InvalidMTDLineException;
import de.openms.knime.mztab.exceptions.InvalidMzTabFormatException;
import de.openms.knime.mztab.exceptions.InvalidMzTabLineException;
import de.openms.knime.mztab.small_molecule.SmallMoleculeMzTabReaderNodeModel;

/**
 * This is the model implementation of MzTabReader. Converts mzTab files into
 * tables holding the small molecule and meta information.
 * 
 * @author The OpenMS Team
 */
public class MzTabReaderNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SmallMoleculeMzTabReaderNodeModel.class);

    private int metaDataRowIdx, smallMolRowIdx;

    /**
     * Create spec for meta data section.
     * 
     * @return {@link DataTableSpec} for the meta data section.
     */
    private DataTableSpec createMetaDataSectionSpec() {
        DataColumnSpec[] colSpecs = new DataColumnSpec[2];
        colSpecs[0] = new DataColumnSpecCreator("fieldname", StringCell.TYPE)
                .createSpec();
        colSpecs[1] = new DataColumnSpecCreator("value", StringCell.TYPE)
                .createSpec();
        return new DataTableSpec(colSpecs);
    }

    /**
     * Constructor for the node model.
     */
    protected MzTabReaderNodeModel() {
        super(new PortType[] { new PortType(IURIPortObject.class) },
                new PortType[] { new PortType(BufferedDataTable.class),
                        new PortType(BufferedDataTable.class) });

        metaDataRowIdx = 1;
        smallMolRowIdx = 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {

        // reset indices ..
        metaDataRowIdx = 1;
        smallMolRowIdx = 1;

        // extract file name
        IURIPortObject obj = (IURIPortObject) inObjects[0];
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
        // container/table small molecule data
        BufferedDataContainer smallMolContainer = null;
        BufferedDataTable smallMolTable = null;
        // container/table for meta data
        BufferedDataContainer metaDataContainer = null;
        BufferedDataTable metaDataTable = null;

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

            // create container for meta data
            metaDataContainer = exec
                    .createDataContainer(createMetaDataSectionSpec());

            // parse MTD lines
            do {
                // skip empty lines
                if (line.trim().length() == 0)
                    continue;

                if (line.trim().length() < 3) {
                    throw new InvalidMzTabFormatException(
                            "Found non-empty line without an identifier.");
                }

                // extract line identifier
                final String identifier = line.trim().substring(0, 3);

                if ("MTD".equals(identifier)) { // handle MTD
                    parseMTDLine(metaDataContainer, line);
                } else if ("SMH".equals(identifier)) { // handle SMH
                    DataTableSpec smSpec = parseSMHeaderLine(line);
                    smallMolContainer = exec.createDataContainer(smSpec);
                } else if ("SML".equals(identifier)) { // handle SML
                    parseSMLLine(smallMolContainer, line);
                }
                // allow knime to cancel node execution
                exec.checkCanceled();
            } while ((line = brReader.readLine()) != null);

            // finalize MTD parsing
            metaDataContainer.close();
            metaDataTable = metaDataContainer.getTable();

            // check if the header is valid
            smallMolContainer.close();
            smallMolTable = smallMolContainer.getTable();
        } catch (CanceledExecutionException e) {
            logger.info("Canceled execution!");
        } finally {
            if (brReader != null)
                brReader.close();
        }

        return new BufferedDataTable[] { metaDataTable, smallMolTable };
    }

    private void parseSMLLine(BufferedDataContainer smallMolDataContainer,
            String line) throws InvalidMzTabLineException,
            InvalidMzTabFormatException {
        String[] line_entries = line.split("\t");

        // check if we already have seen a SMH line
        if (smallMolDataContainer == null) {
            throw new InvalidMzTabFormatException("Found SML before SMH");
        }

        // check if valid
        if (line_entries.length != (smallMolDataContainer.getTableSpec()
                .getNumColumns() + 1)) {
            throw new InvalidMzTabLineException(line);
        }

        DataCell[] cells = parseGenericLine(line_entries, smallMolDataContainer);
        RowKey key = new RowKey("Row " + smallMolRowIdx++);
        DataRow row = new DefaultRow(key, cells);
        smallMolDataContainer.addRowToTable(row);
    }

    private void parseMTDLine(BufferedDataContainer metaDataContainer,
            String line) throws InvalidMTDLineException {
        String[] line_entries = line.split("\t");

        // check if valid
        if (line_entries.length != 3) {
            throw new InvalidMTDLineException(line);
        }

        DataCell[] cells = parseGenericLine(line_entries, metaDataContainer);
        RowKey key = new RowKey("Row " + metaDataRowIdx++);
        DataRow row = new DefaultRow(key, cells);
        metaDataContainer.addRowToTable(row);
    }

    private DataCell[] parseGenericLine(final String[] line_entries,
            BufferedDataContainer container) {
        DataCell[] cells = new DataCell[container.getTableSpec()
                .getNumColumns()];
        // convert the entries into a table column
        for (int i = 0; i < container.getTableSpec().getNumColumns(); ++i) {
            if (container.getTableSpec().getColumnSpec(i).getType() == IntCell.TYPE) {
                if (line_entries[i + 1] == null
                        || "null".equals(line_entries[i + 1])) {
                    cells[i] = new MissingCell(line_entries[i + 1]);
                } else {
                    cells[i] = new IntCell(
                            Integer.parseInt(line_entries[i + 1]));
                }
            } else if (container.getTableSpec().getColumnSpec(i).getType() == DoubleCell.TYPE) {
                // we need to make sure that it is a proper value
                if (line_entries[i + 1] == null
                        || "INF".equals(line_entries[i + 1])
                        || "NaN".equals(line_entries[i + 1])
                        || "null".equals(line_entries[i + 1])) {
                    cells[i] = new MissingCell(line_entries[i + 1]);
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

    private DataTableSpec parseSMHeaderLine(final String line) {
        String[] line_entries = line.split("\t");
        DataColumnSpec[] colSpecs = new DataColumnSpec[line_entries.length - 1];

        for (int i = 1; i < line_entries.length; ++i) {
            DataType type = getDataType(line_entries[i]);
            colSpecs[i - 1] = new DataColumnSpecCreator(line_entries[i], type)
                    .createSpec();
        }

        return new DataTableSpec(colSpecs);
    }

    private DataType getDataType(final String fieldName) {
        if (isSMDouble(fieldName)) {
            return DoubleCell.TYPE;
        } else if (isSMInt(fieldName)) {
            return IntCell.TYPE;
        } else {
            return StringCell.TYPE;
        }
    }

    // smallmolecule_abundance_assay[1-n]
    Pattern regSmallMolAbundanceAssay = Pattern
            .compile("^smallmolecule_abundance_assay\\[\\d*\\]$");
    // smallmolecule_abundance_study_variable[1-n]
    Pattern regSmallMolAbundanceStudyVar = Pattern
            .compile("^smallmolecule_abundance_study_variable\\[\\d*\\]$");
    // smallmolecule_abundance_stdev_study_variable[1-n]
    Pattern regSmallMolAbundanceStdDev = Pattern
            .compile("^smallmolecule_abundance_stdev_study_variable\\[\\d*\\]$");
    // smallmolecule_abundance_std_error_study_variable[1-n]
    Pattern regSmallMolAbundanceStdErr = Pattern
            .compile("^smallmolecule_abundance_std_error_study_variable\\[\\d*\\]$");
    // best_search_engine_score[1-n] ? ParameterList
    Pattern regSmallMolBestSearchEngineScore = Pattern
            .compile("^best_search_engine_score\\[\\d*\\]$");
    // search_engine_score[1-n]_ms_run[1-n] ? ParameterList
    Pattern regSmallMolSearchEngineScoreMsRun = Pattern
            .compile("^search_engine_score\\[\\d*\\]_ms_run\\[\\d*\\]$");

    private boolean isSMDouble(final String fieldName) {
        // retention time ? Double List

        // exp_mass_to_charge
        // calc_mass_to_charge
        // + regex matching
        return "exp_mass_to_charge".equals(fieldName)
                || "calc_mass_to_charge".equals(fieldName)
                || regSmallMolAbundanceAssay.matcher(fieldName).matches()
                || regSmallMolAbundanceStudyVar.matcher(fieldName).matches()
                || regSmallMolAbundanceStdDev.matcher(fieldName).matches()
                || regSmallMolAbundanceStdErr.matcher(fieldName).matches()
                || regSmallMolBestSearchEngineScore.matcher(fieldName)
                        .matches()
                || regSmallMolSearchEngineScoreMsRun.matcher(fieldName)
                        .matches();
    }

    private boolean isSMInt(final String fieldName) {
        // charge
        // taxid
        // reliability
        return "charge".equals(fieldName) || "taxid".equals(fieldName)
                || "reliability".equals(fieldName);
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
        return new DataTableSpec[] { createMetaDataSectionSpec(), null };
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
