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
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;

import com.genericworkflownodes.knime.mime.demangler.DemanglerException;
import com.genericworkflownodes.knime.mime.demangler.IDemangler;

import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArrayList;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.Scan;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

public class MzMLSpectrumImporter implements IDemangler {

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
				new DataColumnSpecCreator("msLevel", IntCell.TYPE).createSpec(),
				new DataColumnSpecCreator("time", DoubleCell.TYPE).createSpec(),
				new DataColumnSpecCreator("m/z array", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec(),
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
		final List<String> spectrumIDs = new ArrayList<String>(um.getSpectrumIDs());
		
		return new Iterator<DataRow>() {
			private int m_index = 0;
			
			@Override
			public boolean hasNext() {
				return m_index < spectrumIDs.size();
			}

			@Override
			public DataRow next() {
				String id = spectrumIDs.get(m_index++);
				try {
					return createSpectrumRow(id, um.getSpectrumById(id));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};
	}
	
	private DataRow createSpectrumRow(String id, Spectrum spectrum) throws DemanglerException {
		int msLevel = 0;
		double time = 0.0;
		for (CVParam p : spectrum.getCvParam()) {
			if (p.getName().equals("ms level")) {
				msLevel = Integer.parseInt(p.getValue());
			}
		}
		List<Scan> scans = spectrum.getScanList().getScan();
		if (scans.size() == 0) {
			throw new DemanglerException("No scan information could be found in the mzML file.");
		}
		Scan scan = scans.get(0);
		for (CVParam p : scan.getCvParam()) {
			if (p.getName().equals("scan start time")) {
				time = Double.parseDouble(p.getValue());
			}
		}
		
		BinaryDataArrayList arraylists = spectrum.getBinaryDataArrayList();
		if (arraylists != null) {
			List<BinaryDataArray> arrays = arraylists.getBinaryDataArray();
			BinaryDataArray mz = null;
			BinaryDataArray intensity = null;
			for (BinaryDataArray bda : arrays) {
				for (CVParam p : bda.getCvParam()) {
					if (p.getName().equals("m/z array")) {
						mz = bda;
					} else if (p.getName().equals("intensity array")) {
						intensity = bda;
					}
				}
			}
			if (mz == null) {
				throw new DemanglerException("The m/z array could not be found in the mzML file.");
			}
			if (intensity == null) {
				throw new DemanglerException("The intensity array could not be found in the mzML file.");
			}
			return new DefaultRow(new RowKey(id), new IntCell(msLevel), new DoubleCell(time),
					MzMLHelper.numberArrayToListCell(mz.getBinaryDataAsNumberArray()),
					MzMLHelper.numberArrayToListCell(intensity.getBinaryDataAsNumberArray()));
		}
		//TODO Warn that no arrays were found? Throw error?
		//For now, return empty arrays, such that at least the metainfo is recorded.
		return new DefaultRow(new RowKey(id), new IntCell(msLevel), new DoubleCell(time),
				MzMLHelper.numberArrayToListCell(new Number[0]),
				MzMLHelper.numberArrayToListCell(new Number[0]));
	}

	@Override
	public void mangle(BufferedDataTable table, URI file) {
		throw new NotImplementedException();
	}

}
