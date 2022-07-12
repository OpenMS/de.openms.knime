package de.openms.knime.importers.mzml;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;

import com.genericworkflownodes.knime.mime.demangler.DemanglerException;
import com.genericworkflownodes.knime.mime.demangler.IDemangler;

import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.Chromatogram;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

public class MzMLChromatogramImporter implements IDemangler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getMIMEType() {
		return "application/x-mzml";
	}

	@Override
	public DataTableSpec getTableSpec() {
		DataTableSpecCreator creator = new DataTableSpecCreator();
		creator.addColumns(
				new DataColumnSpecCreator("time array", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec(),
				new DataColumnSpecCreator("intensity array", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec()
		);
		return creator.createSpec();
	}

	@Override
	public PortObjectSpec getPortOjectSpec() {
		return getTableSpec();
	}

	@Override
	public Iterator<DataRow> demangle(URI file) throws DemanglerException {
		
		URL url = null;
		try {
			url = file.toURL();
		} catch(MalformedURLException e) {
			throw new DemanglerException("The given URI is not a valid URL.", e);
		}
		
		final MzMLUnmarshaller um = new MzMLUnmarshaller(url);
		final List<String> chromIDs = new ArrayList<String>(um.getChromatogramIDs());
		
		return new Iterator<DataRow>() {
			private int m_index = 0;
			
			@Override
			public boolean hasNext() {
				return m_index < chromIDs.size();
			}

			@Override
			public DataRow next() {
				String id = chromIDs.get(m_index++);
				try {
					return createChromatogramRow(id, um.getChromatogramById(id));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};
	}
	
	private DataRow createChromatogramRow(String id, Chromatogram chrom) throws DemanglerException {
		List<BinaryDataArray> arrays = chrom.getBinaryDataArrayList().getBinaryDataArray();
		BinaryDataArray time = null;
		BinaryDataArray intensity = null;
		for (BinaryDataArray bda : arrays) {
			for (CVParam p : bda.getCvParam()) {
				if (p.getName().equals("time array")) {
					time = bda;
				} else if (p.getName().equals("intensity array")) {
					intensity = bda;
				}
			}
		}
		if (time == null) {
			throw new DemanglerException("The time array could not be found in the mzML file.");
		}
		if (intensity == null) {
			throw new DemanglerException("The intensity array could not be found in the mzML file.");
		}
		return new DefaultRow(new RowKey(id),
				MzMLHelper.numberArrayToListCell(time.getBinaryDataAsNumberArray()),
				MzMLHelper.numberArrayToListCell(intensity.getBinaryDataAsNumberArray()));
	}
	
	@Override
	public void mangle(BufferedDataTable table, URI file) {
		throw new NotImplementedException();
	}

}
