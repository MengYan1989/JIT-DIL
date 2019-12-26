package zju.defect.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

public class CSV_handler {

	public List<String[]> getContentFromFile(File file) throws IOException {
		List<String[]> content = new ArrayList<String[]>();
		;
		CsvListReader reader = new CsvListReader(new FileReader(file),
				CsvPreference.EXCEL_PREFERENCE);
		reader.getCSVHeader(true);
		List<String> line = new ArrayList<String>();
		while ((line = reader.read()) != null) {
			content.add(line.toArray(new String[] {}));
		}
		return content;
	}

	public List<String[]> getDetailFromFile(File file) throws IOException {
		List<String[]> content = new ArrayList<String[]>();
		;
		CsvListReader reader = new CsvListReader(new FileReader(file),
				CsvPreference.EXCEL_PREFERENCE);
		String[] header = reader.getCSVHeader(true);
		content.add(header);
		List<String> line = new ArrayList<String>();
		while ((line = reader.read()) != null) {
			content.add(line.toArray(new String[] {}));
		}
		return content;
	}

	public String[] getHeaderFromFile(File file) throws IOException {
		CsvListReader reader = new CsvListReader(new FileReader(file),
				CsvPreference.EXCEL_PREFERENCE);
		return reader.getCSVHeader(true);
	}

	public void writeToCsv(File file, String[] header, List<String[]> content)
			throws IOException {
		if(!file.exists()){
		    file.getParentFile().mkdir();
		}
		
		CsvListWriter writer = new CsvListWriter(new FileWriter(file),
				CsvPreference.EXCEL_PREFERENCE);
		writer.writeHeader(header);
		for (String[] str : content) {
			writer.write(str);
		}
		writer.close();
	}

	public void writeContentToCsv(File file, List<String[]> content)
			throws IOException {
		CsvListWriter writer = new CsvListWriter(new FileWriter(file),
				CsvPreference.EXCEL_PREFERENCE);
		for (String[] str : content) {
			writer.write(str);
		}
		writer.close();
	}


	public void writeHeaderToCsv(File file, String[] header) throws IOException {
		CsvListWriter writer = new CsvListWriter(new FileWriter(file),
				CsvPreference.EXCEL_PREFERENCE);
		writer.writeHeader(header);
		writer.close();
	}

	public String getTxtContent(String url) throws IOException {
		String outputText = "";

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(url), "UTF-8")); // .............
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			outputText += line;
		}
		br.close();
		return outputText;
	}

}