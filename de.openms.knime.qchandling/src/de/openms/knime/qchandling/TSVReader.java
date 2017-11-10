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
package de.openms.knime.qchandling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

/**
 * Helper class to ease the process of reading in the QC TSV files.
 * 
 * @author aiche
 */
public abstract class TSVReader {

    /**
     * Exception indicating that the parsed header doesn't correspond the header
     * structure expected by the TSVReader.
     * 
     * @author aiche
     */
    public static class InvalidHeaderException extends Exception {

        /**
         * The serialVersionUID.
         */
        private static final long serialVersionUID = 3447134484787762192L;

        /**
         * Invalid header was detected by differing number of columns.
         * 
         * @param expected
         *            Expected number of columns.
         * @param actual
         *            Actual number of columns.
         */
        public InvalidHeaderException(int expected, int actual) {
            super("Invalid file header. Expected " + expected
                    + " columns but got " + actual + ".");
        }

        /**
         * Invalid header was detected by differing column header.
         * 
         * @param expected
         *            Expected column header.
         * @param actual
         *            Actual column header.
         */
        public InvalidHeaderException(String expected, String actual) {
            super("Invalid header element: Expected " + expected + " but got "
                    + actual + ".");
        }

        /**
         * General purpose constructor giving a message for the user.
         * 
         * @param message
         *            The message.
         */
        public InvalidHeaderException(String message) {
            super(message);
        }
    }

    /**
     * Exception indicating an invalid line in the tsv file.
     * 
     * @author aiche
     */
    public static class InvalidLineException extends Exception {

        /**
         * The serialVersionUID.
         */
        private static final long serialVersionUID = 8638283665268501023L;

        /**
         * C'tor.
         * 
         * @param lineNumber
         *            The number of the line causing the error.
         * @param offendingLine
         *            The actual line causing the error.
         */
        public InvalidLineException(int lineNumber, String offendingLine) {
            super("Invalid file. Offending line: nr=" + lineNumber + "; "
                    + offendingLine);
        }

        /**
         * C'tor.
         * 
         * @param lineNumber
         *            The number of the line causing the error.
         * @param offendingLine
         *            The actual line causing the error.
         * @param cause
         *            The cause to preserve the stack trace.
         */
        public InvalidLineException(int lineNumber, String offendingLine,
                Throwable cause) {
            super("Invalid file. Offending line: nr=" + lineNumber + "; "
                    + offendingLine, cause);
        }
    }

    /**
     * Construct a TSVReader for the given number of columns.
     * 
     * @param numberOfColumns
     *            The number of expected columns.
     * @param ignoreAdditionalContent
     *            If true additional columns that do not fit to the expected
     *            format are silently ignored instead of generating an error.
     *            Default is false.
     * @param ignoreMissingColumns
     *            If true missing columns are not raising an exception instead
     *            they are filled with empty default values.
     */
    public TSVReader(final int numberOfColumns,
            final boolean ignoreAdditionalContent,
            final boolean ignoreMissingColumns) {
        m_numberOfColumns = numberOfColumns;
        m_ignoreAdditionalContent = ignoreAdditionalContent;
        m_ignoreMissingColumns = ignoreMissingColumns;
    }

    /**
     * Construct a TSVReader for the given number of columns.
     * 
     * @param numberOfColumns
     *            The number of expected columns.
     * @param ignoreAdditionalContent
     *            If true additional columns that do not fit to the expected
     *            format are silently ignored instead of generating an error.
     *            Default is false.
     */
    public TSVReader(final int numberOfColumns,
            final boolean ignoreAdditionalContent) {
        this(numberOfColumns, ignoreAdditionalContent, false);
    }

    /**
     * Construct a TSVReader for the given number of columns.
     * 
     * @param numberOfColumns
     *            The number of expected columns.
     */
    public TSVReader(final int numberOfColumns) {
        this(numberOfColumns, false, false);
    }

    /**
     * The number of columns of the tsv file to read.
     */
    private final int m_numberOfColumns;

    /**
     * Flag indicating if additional columns are ignored or reported as error.
     */
    private final boolean m_ignoreAdditionalContent;

    /**
     * Flag indicating if missing columns are treated as errors.
     */
    private final boolean m_ignoreMissingColumns;

    /**
     * The logger instance.
     */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TSVReader.class);

    /**
     * The TSV separator.
     */
    private static final String SEPARATOR = "\t";

    /**
     * The header of the tsv file to parse.
     * 
     * @return A String array containing all the column headers.
     */
    protected abstract String[] getHeader();

    /**
     * Checks if the header elements found in the file correspond to those
     * defined by the deriving class.
     * 
     * @param header
     *            The header that should be tested.
     * @throws Exception
     *             If the headers do not match.
     */
    private void validateHeader(String headerLine)
            throws InvalidHeaderException {
        if (headerLine == null) {
            throw new InvalidHeaderException(
                    "Could not extract a header from the given file.");
        }

        String[] header = headerLine.trim().split(SEPARATOR, -1);

        // validate if too much columns exist
        if (((header.length > m_numberOfColumns) && !m_ignoreAdditionalContent)) {
            throw new InvalidHeaderException(m_numberOfColumns, header.length);
        }
        // validate if enough columns exist
        if ((header.length < m_numberOfColumns) && !m_ignoreMissingColumns) {
            throw new InvalidHeaderException(m_numberOfColumns, header.length);
        }

        // validate the actual columns
        for (int i = 0; i < m_numberOfColumns && i < header.length; ++i) {
            if (!header[i].equals(getHeader()[i])) {
                throw new InvalidHeaderException(getHeader()[i], header[i]);
            }
        }
    }

    /**
     * The parse method extracting the different values for the current line.
     * 
     * @param tokens
     *            An array of Strings containing the values that should be
     *            extracted.
     * @return The individual values of the current line converted into
     *         DataCells.
     */
    protected abstract DataCell[] parseLine(String[] tokens);

    /**
     * Parses the tsv given file and adds it's content to the given container.
     * The ExecutionContext is used to monitor for cancelled execution.
     * 
     * @param tsvFile
     *            The tsv file to parse.
     * @param container
     *            The container where the data should be added.
     * @param exec
     *            The current execution context to indicate
     * @throws IOException
     *             In case IO operations fail.
     * @throws InvalidLineException
     *             If one of the lines in the file is invalid.
     * @throws CanceledExecutionException
     *             If the node execution was cancelled.
     * @throws InvalidHeaderException
     *             If the header of the file does not correspond to the expected
     *             header.
     */
    public void run(File tsvFile, BufferedDataContainer container,
            final ExecutionContext exec) throws IOException,
            InvalidLineException, CanceledExecutionException,
            InvalidHeaderException {
        BufferedReader brReader = null;
        try {
            // read the data and fill the table
            brReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(tsvFile), "UTF-8"));

            // skip but check the header
            String header = brReader.readLine();
            validateHeader(header);

            // for all lines
            String line;
            int rowIdx = 1;

            while ((line = brReader.readLine()) != null) {
                // skip empty line
                if ("".equals(line.trim())) {
                    continue;
                }

                String[] tokens = line.trim().split(SEPARATOR, -1);

                try {
                    // we try to parse and leave it to the deriving class to
                    // check if everything is correct
                    DataCell[] cells = parseLine(tokens);

                    RowKey key = new RowKey("Row " + rowIdx);
                    DataRow row = new DefaultRow(key, cells);
                    container.addRowToTable(row);
                } catch (Exception ex) {
                    throw new InvalidLineException(rowIdx, line, ex);
                }

                exec.checkCanceled();
                ++rowIdx;
            }
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
            throw ex;
        } finally {
            if (brReader != null) {
                brReader.close();
            }
        }
    }
}
