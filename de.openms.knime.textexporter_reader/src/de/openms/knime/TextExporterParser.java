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
package de.openms.knime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Parser for text based feature / peptide ID representations as exported by OpenMS'
 * TextExporter.
 * 
 * @author aiche
 */
public class TextExporterParser {

	// the logger instance
	private static final NodeLogger logger = NodeLogger
			.getLogger(TextExporterParser.class);

	/**
	 * The separator used in the file
	 */
	private String m_separator;

	/**
	 * The element to parse feature / consensus / only IDs
	 */
	private String m_elementOfInterest;
	
	private boolean m_peponlymode = false;

	/**
	 * 
	 * @param elementOfInterest
	 */
	public TextExporterParser(final String elementOfInterest) {
		m_separator = " ";
		m_elementOfInterest = elementOfInterest;
	}

	/**
	 * Try to determine the separator in this file based on one of the header
	 * lines.
	 * 
	 * @param line
	 *            The line to guess the separator from.
	 */
	private void guessSeparator(final String line) {
		if (line.startsWith("#" + m_elementOfInterest + m_separator + "rt")) {
			// it seems to be the default one " "
			return;
		}

		String[] potential_separators = new String[] { "\t", ";", "," };
		for (String separator : potential_separators) {
			if (line.startsWith("#" + m_elementOfInterest + separator + "rt") || line.startsWith("#rt" + separator)) {
				m_separator = separator;
				logger.debug("New separator chosen: '" + m_separator + "'");
				break;
			}
		}
	}

	public BufferedDataTable parseFile(File inputFile,
			final ExecutionContext exec) throws Exception, IOException {
		BufferedReader brReader = null;
		DataTableSpec spec = null;
		BufferedDataContainer container = null;
		BufferedDataTable out = null;
		try {
			// read the data and fill the table
			brReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(inputFile)));

			// find start point
			String line;
			String lastheaderline = "";
			boolean hasNonPeptideHeader = false;
			while ((line = brReader.readLine()) != null) {
				if (line.startsWith("#" + m_elementOfInterest)) {
					spec = parseDataTableSpec(line);
					container = exec.createDataContainer(spec);
				}
				if (line.startsWith("#MAP") || line.startsWith("#FEATURE") || line.startsWith("#CONSENSUS"))
				{
					hasNonPeptideHeader = true;
				}
				
				if (!line.startsWith("#"))
				{
					break;
				}
				else
				{
					lastheaderline = line;
				}
			}

			// If no "header" for this element type (here PEPTIDE) was found
			// assume a standard one
			if (container == null)
			{
				if (!hasNonPeptideHeader)
				{
					m_peponlymode = true;
					spec = parseDataTableSpec(lastheaderline);
					container = exec.createDataContainer(spec);		
				} else {
					throw new Exception("No peptide data found. Run TextExporter without no_id and without proteins_only.");
				}
			}
			
			// now parse the content
			int rowIdx = 1;
			if ("PEPTIDE".equals(m_elementOfInterest)) {
				while ((line = brReader.readLine()) != null) {
					if (line.startsWith(m_elementOfInterest) || m_peponlymode) {
							container.addRowToTable(parseLine(spec, container,
									"", line, rowIdx++));
					}
					exec.checkCanceled();
				}
			}
			else {
				String lastConsensusLine = null;			
				while ((line = brReader.readLine()) != null) {
					if (line.startsWith(m_elementOfInterest)) {
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
		return out;
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
	 * @throws Exception 
	 */
	private DataRow parseLine(DataTableSpec spec,
			BufferedDataContainer container, String consensusLine,
			String peptideLine, int rowIdx) throws Exception {
		// create a new Row
		DataCell[] cells = new DataCell[spec.getNumColumns()];

		// get the values
		String[] consensusValues = consensusLine.split(m_separator, -1);
		String[] peptideValues = peptideLine.split(m_separator, -1);
		
		int offset = m_peponlymode ? 0 : 1;
		int n_pepinfocols = 13;
		
		int endoffset = 0; // if weird pt and rt prediction values are appended
		if ("".equals(peptideLine.trim())) {
			peptideValues = new String[] { "PEPTIDE", "0", "0", "0", "-1",
					"UNIDENTIFIED_PEPTIDE", "0", "", "", "", "",
					"UNIDENTIFIED_PROTEIN", "", "" };
		} else {
			if (peptideValues.length - 4 == n_pepinfocols + offset)
			{
				endoffset = 4;
			} else if (peptideValues.length - 2 == n_pepinfocols + offset) {
				endoffset = 2;
			} else if (peptideValues.length == n_pepinfocols + offset){
				endoffset = 0;
			} else {
				throw new Exception("Length of potential PEPTIDE line does not match any possible known formats. Do not use no_ids in TextExporter.");
			}
		}
		
		if ("PEPTIDE".equals(m_elementOfInterest)) {
			for (int i = 0; i < peptideValues.length - endoffset - offset; ++i) {
				String pValue = (!("nan".equals(peptideValues[i+offset])) ? peptideValues[i+offset]
						: "0");

				if (spec.getColumnSpec(i).getType() == IntCell.TYPE) {
					cells[i] = new IntCell(Integer.parseInt(pValue));
				} else if (spec.getColumnSpec(i).getType() == DoubleCell.TYPE) {
					cells[i] = new DoubleCell(Double.parseDouble(pValue));
				} else {
					cells[i] = new StringCell(pValue);
				}
			}

			RowKey key = new RowKey("Row " + rowIdx);
			return new DefaultRow(key, cells);
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
			String pValue = (!("nan".equals(peptideValues[i])) ? peptideValues[i]
					: "0");

			if (spec.getColumnSpec(cur_idx).getType() == IntCell.TYPE) {
				cells[cur_idx] = new IntCell(Integer.parseInt(pValue));
			} else if (spec.getColumnSpec(cur_idx).getType() == DoubleCell.TYPE) {
				cells[cur_idx] = new DoubleCell(Double.parseDouble(pValue));
			} else {
				cells[cur_idx] = new StringCell(pValue);
			}
		}

		RowKey key = new RowKey("Row " + rowIdx);
		return new DefaultRow(key, cells);
	}

	private DataTableSpec parseDataTableSpec(String line) {
		
		guessSeparator(line);

		DataColumnSpec[] specs = null;
		int current_col = 0;
		int n_pepinfo_cols = 13;
		if (!"PEPTIDE".equals(m_elementOfInterest)) {
			String[] colHeaders = line.split(m_separator);
			// we add also peptide information
			// #PEPTIDE rt mz score rank sequence charge aa_before aa_after 
			// score_type search_identifier accessions start end
			specs = new DataColumnSpec[colHeaders.length - 1 + n_pepinfo_cols];

			for (int i = 1; i < colHeaders.length; ++i) {
				if (colHeaders[i].startsWith("charge_")) {
					specs[i - 1] = new DataColumnSpecCreator(colHeaders[i],
							IntCell.TYPE).createSpec();
				} else {
					specs[i - 1] = new DataColumnSpecCreator(colHeaders[i],
							DoubleCell.TYPE).createSpec();
				}
			}
			current_col = colHeaders.length - 1;
		}
		else
		{
			specs = new DataColumnSpec[n_pepinfo_cols];
		}

		// add peptide information
		specs[current_col++] = new DataColumnSpecCreator("peptide_rt",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("peptide_mz",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("score",
				DoubleCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("rank",
				IntCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("sequence",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("peptide_charge",
				IntCell.TYPE).createSpec();
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
		specs[current_col++] = new DataColumnSpecCreator("start",
				StringCell.TYPE).createSpec();
		specs[current_col++] = new DataColumnSpecCreator("end",
				StringCell.TYPE).createSpec();

		return new DataTableSpec(specs);
	}
}
