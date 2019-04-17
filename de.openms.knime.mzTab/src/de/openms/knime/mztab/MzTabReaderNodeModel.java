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
import java.util.ArrayList;
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
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.BooleanCell;
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

/**
 * This is the model implementation of MzTabReader. Converts mzTab files into
 * tables holding the small molecule and meta information.
 * 
 * @author The OpenMS Team
 */
public class MzTabReaderNodeModel extends NodeModel {

    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(MzTabReaderNodeModel.class);

    private int metaDataRowIdx, proteinRowIdx, peptideRowIdx, psmRowIdx, smallMolRowIdx;

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
		super(new PortType[] { IURIPortObject.TYPE },
				new PortType[] {
						BufferedDataTable.TYPE,
						BufferedDataTable.TYPE,
						BufferedDataTable.TYPE, 
						BufferedDataTable.TYPE,
						BufferedDataTable.TYPE 
						});

        metaDataRowIdx = 1;
        proteinRowIdx = 1;
        peptideRowIdx = 1;
        psmRowIdx = 1;
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
        proteinRowIdx = 1;
        peptideRowIdx = 1;
        psmRowIdx = 1;
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
        
        // container/table for meta data
        BufferedDataContainer metaDataContainer = null;
        BufferedDataTable metaDataTable = null;
        // container/table for meta data
        BufferedDataContainer proteinDataContainer = null;
        BufferedDataTable proteinDataTable = null;
        // container/table for meta data
        BufferedDataContainer peptideDataContainer = null;
        BufferedDataTable peptideDataTable = null;
        // container/table for meta data
        BufferedDataContainer psmDataContainer = null;
        BufferedDataTable psmDataTable = null;
        // container/table small molecule data
        BufferedDataContainer smallMolContainer = null;
        BufferedDataTable smallMolTable = null;


        try {
            // read the data and fill the table
            brReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(cXMLFile)));

            // validate mzTab starting point
            String line = brReader.readLine();

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
                } else if ("PRH".equals(identifier)) { // handle PRH
                    DataTableSpec proteinSpec = parseHeaderLine(line);
                    proteinDataContainer = exec.createDataContainer(proteinSpec);
                } else if ("PRT".equals(identifier)) { // handle PRT
                    parsePRTLine(proteinDataContainer, line);
                } else if ("PEH".equals(identifier)) { // handle PEH
                    DataTableSpec pepSpec = parseHeaderLine(line);
                    peptideDataContainer = exec.createDataContainer(pepSpec);
                } else if ("PEP".equals(identifier)) { // handle PEP
                    parsePEPLine(peptideDataContainer, line);
                } else if ("PSH".equals(identifier)) { // handle PSH
                    DataTableSpec psmSpec = parseHeaderLine(line);
                    psmDataContainer = exec.createDataContainer(psmSpec);
                } else if ("PSM".equals(identifier)) { // handle PSM
                    parsePSMLine(psmDataContainer, line);
                } else if ("SMH".equals(identifier)) { // handle SMH
                    DataTableSpec smSpec = parseHeaderLine(line);
                    smallMolContainer = exec.createDataContainer(smSpec);
                } else if ("SML".equals(identifier)) { // handle SML
                    parseSMLLine(smallMolContainer, line);
                }
                // allow knime to cancel node execution
                exec.checkCanceled();
            } while ((line = brReader.readLine()) != null);
                        
            if (proteinDataContainer == null) proteinDataContainer = exec.createDataContainer(createEmptySpec());
            if (peptideDataContainer == null) peptideDataContainer =  exec.createDataContainer(createEmptySpec());
            if (psmDataContainer == null)  psmDataContainer = exec.createDataContainer(createEmptySpec());
            if (smallMolContainer == null) smallMolContainer =  exec.createDataContainer(createEmptySpec());

            // finalize MTD parsing
            metaDataContainer.close();
            metaDataTable = metaDataContainer.getTable();
            // finalize PRT parsing
            proteinDataContainer.close();
            proteinDataTable = proteinDataContainer.getTable();
            // finalize PEP parsing
            peptideDataContainer.close();
            peptideDataTable = peptideDataContainer.getTable();
            // finalize PSM parsing
            psmDataContainer.close();
            psmDataTable = psmDataContainer.getTable();
            
            // check if the header is valid
            smallMolContainer.close();
            smallMolTable = smallMolContainer.getTable();
        } catch (CanceledExecutionException e) {
            logger.info("Canceled execution!");
        } finally {
            if (brReader != null)
                brReader.close();
        }

        return new BufferedDataTable[] { metaDataTable, proteinDataTable, peptideDataTable, psmDataTable, smallMolTable };
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
    
    private void parsePRTLine(BufferedDataContainer proteinDataContainer,
            String line) throws InvalidMzTabLineException,
            InvalidMzTabFormatException {
        String[] line_entries = line.split("\t");

        // check if we already have seen a PRT line
        if (proteinDataContainer == null) {
            throw new InvalidMzTabFormatException("Found PRT before PRH");
        }

        // check if valid
        if (line_entries.length != (proteinDataContainer.getTableSpec()
                .getNumColumns() + 1)) {
            throw new InvalidMzTabLineException(line);
        }

        DataCell[] cells = parseGenericLine(line_entries, proteinDataContainer);
        RowKey key = new RowKey("Row " + proteinRowIdx++);
        DataRow row = new DefaultRow(key, cells);
        proteinDataContainer.addRowToTable(row);
    }
    
    
    private void parsePEPLine(BufferedDataContainer peptideDataContainer,
            String line) throws InvalidMzTabLineException,
            InvalidMzTabFormatException {
        String[] line_entries = line.split("\t");

        // check if we already have seen a PEP line
        if (peptideDataContainer == null) {
            throw new InvalidMzTabFormatException("Found PEP before PEH");
        }

        // check if valid
        if (line_entries.length != (peptideDataContainer.getTableSpec()
                .getNumColumns() + 1)) {
            throw new InvalidMzTabLineException(line);
        }

        DataCell[] cells = parseGenericLine(line_entries, peptideDataContainer);
        RowKey key = new RowKey("Row " + peptideRowIdx++);
        DataRow row = new DefaultRow(key, cells);
        peptideDataContainer.addRowToTable(row);
    }
    
    private void parsePSMLine(BufferedDataContainer psmDataContainer,
            String line) throws InvalidMzTabLineException,
            InvalidMzTabFormatException {
        String[] line_entries = line.split("\t");

        // check if we already have seen a PSM line
        if (psmDataContainer == null) {
            throw new InvalidMzTabFormatException("Found PSM before PSH");
        }

        // check if valid
        if (line_entries.length != (psmDataContainer.getTableSpec()
                .getNumColumns() + 1)) {
            throw new InvalidMzTabLineException(line);
        }

        DataCell[] cells = parseGenericLine(line_entries, psmDataContainer);
        RowKey key = new RowKey("Row " + psmRowIdx++);
        DataRow row = new DefaultRow(key, cells);
        psmDataContainer.addRowToTable(row);
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
    

    private DataCell[] parseGenericLine(final String[] line_entries,
            BufferedDataContainer container) {
        DataCell[] cells = new DataCell[container.getTableSpec()
                .getNumColumns()];
        // convert the entries into a table column
        for (int i = 0; i < container.getTableSpec().getNumColumns(); ++i) {
            if (container.getTableSpec().getColumnSpec(i).getType() == IntCell.TYPE) {
                if (line_entries[i + 1] == null
                        || "null".equals(line_entries[i + 1])
                        || "-".equals(line_entries[i + 1])) {
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
                        || "null".equals(line_entries[i + 1])
                        || "-".equals(line_entries[i + 1])) {
                    cells[i] = new MissingCell(line_entries[i + 1]);
                } else {
                    cells[i] = new DoubleCell(
                            Double.parseDouble(line_entries[i + 1]));
                }
            } else if (container.getTableSpec().getColumnSpec(i).getType() == ListCell.getCollectionType(DoubleCell.TYPE)) {
                // we need to make sure that it is a proper value
                if (line_entries[i + 1] == null
                        || "null".equals(line_entries[i + 1])
                        || "-".equals(line_entries[i + 1])) {
                    cells[i] = new MissingCell(line_entries[i + 1]);
                } else {
                	ArrayList<DoubleCell> lc = new ArrayList<DoubleCell>();
                	//escaping the pipe. TODO Think about putting separators and missing values as constant values.
                	for (String dstr : line_entries[i + 1].split("\\|"))
                	{
                		double d = Double.parseDouble(dstr);
                		lc.add(new DoubleCell(d));
                	}
                	ListCell outputCell = CollectionCellFactory.createListCell(lc);
                	cells[i] = outputCell;
                }
            } else if (container.getTableSpec().getColumnSpec(i).getType() == BooleanCell.TYPE) {
                // we need to make sure that it is a proper value
                if (line_entries[i + 1] == null
                        || "null".equals(line_entries[i + 1])
                        || "-".equals(line_entries[i + 1])) {
                    cells[i] = new MissingCell(line_entries[i + 1]);
                } else {
                	BooleanCell cell = BooleanCell.FALSE;
                	if (convertToBoolean(line_entries[i + 1])) {
                		cell = BooleanCell.TRUE;
                	}
                	cells[i] = cell;
                }
            } else {
                // it is a string value -> just put it into the
                // table
                cells[i] = new StringCell(line_entries[i + 1]);
            }
        }
        return cells;
    }
    
    private boolean convertToBoolean(String value) {
        boolean returnValue = false;
        if ("1".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || 
            "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value))
            returnValue = true;
        return returnValue;
    }

    private DataTableSpec parseHeaderLine(final String line) {
        String[] line_entries = line.split("\t");
        DataColumnSpec[] colSpecs = new DataColumnSpec[line_entries.length - 1];

        for (int i = 1; i < line_entries.length; ++i) {
            DataType type = getDataType(line_entries[i]);
            colSpecs[i - 1] = new DataColumnSpecCreator(line_entries[i], type)
                    .createSpec();
        }

        return new DataTableSpec(colSpecs);
    }
    
    private DataTableSpec createEmptySpec() {
        return new DataTableSpec(new DataColumnSpec[0]);
    }
    
    private DataType getDataType(final String fieldName) {
    	String trimmed = fieldName.trim();
        if (isDouble(trimmed)) {
            return DoubleCell.TYPE;
        } else if (isInt(trimmed)) {
            return IntCell.TYPE;
        } else if (isBool(trimmed)) {
            return BooleanCell.TYPE;
        } else if (isDoubleList(trimmed)) {
            return ListCell.getCollectionType(DoubleCell.TYPE);
        } else {
            return StringCell.TYPE;
        }
    }

    // (best_)search_engine_score_...
    Pattern regSearchEngineScore = Pattern
            .compile("^(?!opt_).+.*search_engine_score.*$");
    // "entitiy"_abundance_"measure"_studyVariable[1-n]
    Pattern regAbundance = Pattern
            .compile("^(?!opt_).+.*_abundance.*$");
    // num_* for number of peptides/psms/etc.
    Pattern regNumberOf = Pattern
            .compile("^num_.*$");

    private boolean isDouble(final String fieldName) {
        return "exp_mass_to_charge".equals(fieldName)
                || "calc_mass_to_charge".equals(fieldName)
                || "mass_to_charge".equals(fieldName)
                || "protein_coverage".equals(fieldName)
                || regAbundance.matcher(fieldName).matches()
                || regSearchEngineScore.matcher(fieldName)
                        .matches();
    }
    
    private boolean isDoubleList(final String fieldName) {
        return "retention_time".equals(fieldName)
                || "retention_time_window".equals(fieldName);
    }

    private boolean isInt(final String fieldName) {
        return "charge".equals(fieldName)
        	|| "taxid".equals(fieldName)
        	|| "start".equals(fieldName)
        	|| "end".equals(fieldName)
            || "reliability".equals(fieldName)
            || regNumberOf.matcher(fieldName).matches();
    }
    
    private boolean isBool(final String fieldName) {
        return "unique".equals(fieldName);
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
        return new DataTableSpec[] { createMetaDataSectionSpec(), null, null, null, null };
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
