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
package de.openms.knime.consensusTextReader;

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

/**
 * This is the model implementation of ConsensusTextReader.
 * 
 * 
 * @author Stephan Aiche and the OpenMS Team
 */
public class ConsensusTextReaderNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(ConsensusTextReaderNodeModel.class);

	/**
	 * The separator used in the file
	 */
	private String m_separator;

	/**
	 * Constructor for the node model.
	 */
	protected ConsensusTextReaderNodeModel() {
		super(new PortType[] { new PortType(URIPortObject.class) },
				new PortType[] { new PortType(BufferedDataTable.class) });
		m_separator = " ";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final PortObject[] inObjects,
			final ExecutionContext exec) throws Exception {

		URIPortObject obj = (URIPortObject) inObjects[0];
		List<URIContent> uris = obj.getURIContents();
		if (uris.size() == 0) {
			throw new Exception(
					"No URI was supplied in URIPortObject at input port 0");
		} else if (uris.size() != 1) {
			throw new Exception(String.format(
					"We can only demangle a single file but got %d.",
					uris.size()));
		}

		URI relURI = uris.get(0).getURI();
		File cXMLFile = new File(relURI);

		BufferedReader brReader = null;
		DataTableSpec spec = null;
		BufferedDataContainer container = null;
		BufferedDataTable out = null;
		try {
			// read the data and fill the table
			brReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(cXMLFile)));

			// find consensus start point
			String line;
			while ((line = brReader.readLine()) != null) {
				if (line.startsWith("#CONSENSUS")) {
					spec = parseDataTableSpec(line);
					container = exec.createDataContainer(spec);
				}
				if (!line.startsWith("#"))
					break;
			}

			int rowIdx = 1;
			String lastConsensusLine = null;

			// now parse the content
			while ((line = brReader.readLine()) != null) {
				if (line.startsWith("CONSENSUS")) {
					// we still have an unparsed last consensus line
					if (lastConsensusLine != null) {
						container.addRowToTable(parseLine(spec, container,
								lastConsensusLine, "", rowIdx++));
					}
					lastConsensusLine = line;
				} else if (line.startsWith("PEPTIDE")) {
					if (lastConsensusLine != null) {
						container.addRowToTable(parseLine(spec, container,
								lastConsensusLine, line, rowIdx++));

						// clear last consensus for next round
						lastConsensusLine = null;
					} else {
						logger.info("Found two identifications for last consensus element. Will ignore second.");
					}
				}
				exec.checkCanceled();
			}

			// ensure that there is no unfinished consensus line
			if (lastConsensusLine != null) {
				container.addRowToTable(parseLine(spec, container,
						lastConsensusLine, "", rowIdx++));
			}

			container.close();
			out = container.getTable();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			throw ex;
		} finally {
			if (brReader != null)
				brReader.close();
		}

		return new BufferedDataTable[] { out };
	}

	/**
	 * Converts the given line from the textexporter file into a row in the
	 * DataContainer.
	 * 
	 * @param exec
	 *            The execution
	 * @param spec
	 * @param container
	 * @param consensusLine
	 * @param rowIdx
	 * @return
	 * @throws CanceledExecutionException
	 */
	private DataRow parseLine(DataTableSpec spec,
			BufferedDataContainer container, String consensusLine,
			String peptideLine, int rowIdx) throws CanceledExecutionException {
		// create a new Row
		DataCell[] cells = new DataCell[spec.getNumColumns()];

		// get the values
		String[] consensusValues = consensusLine.split(m_separator);
		String[] peptideValues = peptideLine.split(m_separator);

		if ("".equals(peptideLine.trim())) {
			peptideValues = new String[] { "PEPTIDE", "0", "0", "0", "-1",
					"UNIDENTIFIED_PEPTIDE", "0", "", "", "", "",
					"UNIDENTIFIED_PROTEIN" };
		}

		// check that the consensus line fits
		assert (consensusValues.length - 1 + peptideValues.length - 1) == spec
				.getNumColumns();
		for (int i = 1; i < consensusValues.length; ++i) {
			String pValue = (!("nan".equals(consensusValues[i])) ? consensusValues[i]
					: "0");

			if (spec.getColumnSpec(i - 1).getType() == IntCell.TYPE) {
				cells[i - 1] = new IntCell(Integer.parseInt(pValue));
			} else {
				cells[i - 1] = new DoubleCell(Double.parseDouble(pValue));
			}
		}

		for (int i = 1; i < peptideValues.length; ++i) {
			// index
			int cur_idx = consensusValues.length + i - 2;

			if (spec.getColumnSpec(cur_idx).getType() == IntCell.TYPE) {
				cells[cur_idx] = new IntCell(Integer.parseInt(peptideValues[i]));
			} else if (spec.getColumnSpec(cur_idx).getType() == DoubleCell.TYPE) {
				cells[cur_idx] = new DoubleCell(
						Double.parseDouble(peptideValues[i]));
			} else {
				cells[cur_idx] = new StringCell(peptideValues[i]);
			}
		}

		RowKey key = new RowKey("Row " + rowIdx);
		return new DefaultRow(key, cells);
	}

	private DataTableSpec parseDataTableSpec(String line) {
		guessSeparator(line);

		String[] colHeaders = line.split(m_separator);
		// we add also peptide information
		// #PEPTIDE rt mz score rank sequence charge aa_before aa_after
		// score_type search_identifier accessions
		DataColumnSpec[] specs = new DataColumnSpec[colHeaders.length + 10];

		for (int i = 1; i < colHeaders.length; ++i) {
			if (colHeaders[i].startsWith("charge_")) {
				specs[i - 1] = new DataColumnSpecCreator(colHeaders[i],
						IntCell.TYPE).createSpec();
			} else {
				specs[i - 1] = new DataColumnSpecCreator(colHeaders[i],
						DoubleCell.TYPE).createSpec();
			}
		}

		// add peptide information
		int current_col = colHeaders.length - 1;
		specs[current_col++] = new DataColumnSpecCreator("peptide_rt",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("peptide_mz",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("score",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("rank", IntCell.TYPE)
				.createSpec();
		specs[current_col++] = new DataColumnSpecCreator("sequence",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("charge", IntCell.TYPE)
				.createSpec();
		specs[current_col++] = new DataColumnSpecCreator("aa_before",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("aa_after",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("score_type",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("search_identifier",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("accessions",
				StringCell.TYPE).createSpec();

		return new DataTableSpec(specs);
	}

	/**
	 * Try to determine the separator in this file based on one of the header
	 * lines.
	 * 
	 * @param line
	 *            The line to guess the separator from.
	 */
	private void guessSeparator(final String line) {
		if (line.startsWith("#CONSENSUS" + m_separator + "rt_cf")) {
			// it seems to be the default one " "
			return;
		}

		String[] potential_separators = new String[] { "\t", ";", "," };
		for (String separator : potential_separators) {
			if (line.startsWith("#CONSENSUS" + separator + "rt_cf")) {
				m_separator = separator;
				logger.debug("New separator chosen: '" + m_separator + "'");
				break;
			}
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
