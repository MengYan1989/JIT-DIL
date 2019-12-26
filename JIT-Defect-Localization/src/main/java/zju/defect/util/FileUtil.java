package zju.defect.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import zju.defect.locater.DefectLocater;

public class FileUtil {
	private static CSV_handler myCSV = new CSV_handler();
	public static List<String> readLinesFromFile(String filePath){
		
		BufferedReader reader = null;
		List<String> lines = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(new File(filePath)));
			
			String line = null;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return lines;

	}
	
	public static void writeLinesToFile(List<String> lines,String filePath){
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filePath)));
			for(String line:lines){
				writer.write(line);
				writer.newLine();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String[] combineStringVector (String[] stringV1, String[] stringV2){
		int size1 = stringV1.length;
		int size2 = stringV2.length;
		
		String[] combineString = new String[size1+size2];
		
		for (int i=0; i<size1; i++){
			String tempContent = stringV1[i];
			combineString[i] = tempContent;
		}
		
		for (int i=0; i<size2; i++){
			String tempContent = stringV2[i];
			combineString[size1+i] = tempContent;
		}
		
		return combineString;
	}

	public static void printMap(Map<Double, Double> entropyF1Map, String mapPath, String[] header) throws IOException {
		// TODO Auto-generated method stub
		//Set<Entry<Double, Double>>  entropyF1s = entropyF1Map.entrySet();
		List<String[]> entropyF1Tuning = new ArrayList<String[]>();
		Iterator iter = entropyF1Map.entrySet().iterator(); 
		while(iter.hasNext()) {
			Entry entry = (Entry)iter.next();
			String[] thisEntropyF1 = new String[2];
			thisEntropyF1[0] = entry.getKey().toString();
			thisEntropyF1[1] = entry.getValue().toString();
			entropyF1Tuning.add(thisEntropyF1);
		}
		
		myCSV.writeToCsv(new File(mapPath), header, entropyF1Tuning);
	}

	public static double GetCommitSizeOfMultiLines(List<String[]> testLines) {
		// TODO Auto-generated method stub
		
		
		Set<String> uniqueCommits = new HashSet<String>();
		
		for (int i=0; i<testLines.size(); i++){
			String[] thisLine = testLines.get(i);
			String commitId = thisLine[0];
			uniqueCommits.add(commitId);
		}
		
		return uniqueCommits.size();
	}

	public static Set<String> SubSetCommits(Set<String> allCommits, Set<String> subCommits) {
		// TODO Auto-generated method stub
		Set<String> subSet = new HashSet<String>();
		
		Iterator<String> it = allCommits.iterator();  
		while (it.hasNext()) {  
		  String str = it.next();  
		  if (!subCommits.contains(str)){
			  subSet.add(str);
		  }
		}		
		return subSet;
	}

	

	public static List<String[]> CombineList(List<String[]> master, List<String[]> sub) {
		// TODO Auto-generated method stub
		for (int i=0; i<sub.size(); i++){
			String[] thisItem = sub.get(i);
			master.add(thisItem);
		}
		
		return master;
	}
	
	public static List<String> CombineListString(List<String> master, List<String> sub) {
		// TODO Auto-generated method stub
		for (int i=0; i<sub.size(); i++){
			String thisItem = sub.get(i);
			master.add(thisItem);
		}
		
		return master;
	}
	
	public static final String[] keyWords = { "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
			"class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
			"new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
			"true", "false", "null" };
	
	public static final String[] keyWordsForFilter = { "@", "if", "boolean", "char", "class", "for", "try", "import",
			"catch", "new", "package", "private", "public", "throw", "throws","assert", "while"};

	public static String PreprocessCode(String lineContent) {
		// TODO Auto-generated method stub
		String processedLine = lineContent;
		//for (int j=0; j<keyWordsForFilter.length; j++)
			//processedLine = processedLine.replace(keyWordsForFilter[j], "");
		if (lineContent.indexOf("/*") > -1) {
			processedLine = lineContent.replace("/*", "");			
			processedLine = processedLine.replace("*/", "");
			processedLine = processedLine.replace("/**", "");
			processedLine = processedLine.replace("**/", "");			
		}
		
		if (lineContent.indexOf("//")>0) {
			int commentIndex = lineContent.indexOf("//");
			processedLine = lineContent.substring(0, commentIndex);		
		}
		
		
		
		//processedLine = StringFilter(processedLine);
		return processedLine;
	}
	
	public static String StringFilter(String str) throws PatternSyntaxException {
		
		String outString = str.replaceAll("\\d+","");
		//String outString = str.replaceAll("\\p{Punct}","");
		return outString;
	}

	public static void AdjustEntropyByLineType(String entropyTrainResultCSVCleanNgram, String entropyTestResultCSVCleanNgram,
			String entropyTestResultCSVCleanNgramAdjusted) throws IOException {
		// TODO Auto-generated method stub
		
		List<String[]> originalResultsGenerateTypeValues = myCSV.getContentFromFile(new File(entropyTrainResultCSVCleanNgram));
		List<String[]> originalResultsForAdjust = myCSV.getContentFromFile(new File(entropyTestResultCSVCleanNgram));
		List<String[]> adjustedResult = new ArrayList<String[]>();
		
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultCSVCleanNgram));
		Map<String, List<Double>> typeValues = new HashMap<String, List<Double>>();
		Map<Integer, String> commitType = new HashMap<Integer, String>();
		for (int i=0; i<originalResultsGenerateTypeValues.size(); i++){
			String[] thisLine = originalResultsGenerateTypeValues.get(i);
			String thisContent = thisLine[1];
			String type = GetType(thisContent);
			double entropy = Double.parseDouble(thisLine[9]);
			if (typeValues.containsKey(type)){
			}
			else{
				List<Double> values = new ArrayList<Double>();
				typeValues.put(type, values);				
			}
			typeValues.get(type).add(entropy);
			commitType.put(i, type);
		}
		
		Map<String, Double> typeAveValues = new HashMap<String, Double>();
		Map<String, Double> typeStdValues = new HashMap<String, Double>();
		for (String type : typeValues.keySet()){
			List<Double> values = typeValues.get(type);
			double ave = values.stream().collect(Collectors.averagingDouble(i -> i));
			double std = 0;
			if (values.size()>0){
				std = values.stream().map(p -> (p - ave) * (p - ave)).collect(Collectors.summingDouble(i -> i)) / values.size();
			    std = Math.sqrt(std);
			}
			typeAveValues.put(type, ave);
			typeStdValues.put(type, std);
			System.out.println(type+"; AVE: " +ave+ "; STD:" + std);
		}
		
		for (int i=0; i<originalResultsForAdjust.size(); i++){
			String[] thisLine = originalResultsForAdjust.get(i);
			String thisContent = thisLine[1];
			String type = GetType(thisContent);
			double ave = 0;
			double std = 0;
			if(typeAveValues.containsKey(type)){
			 ave = typeAveValues.get(type);
			 std = typeStdValues.get(type);
			}
			
			double entropy = Double.parseDouble(thisLine[9]);
            double adjustedEntropy = 0;
            if (std > 0)
               adjustedEntropy = (entropy - ave)/std;
			thisLine[9] = Double.toString(adjustedEntropy);
            adjustedResult.add(thisLine);			
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCleanNgramAdjusted), header, adjustedResult);		
	}

	private static String GetType(String thisContent) {
        String type = "";
		boolean containKey = false;
        for (int j=0; j<keyWords.length; j++){
		    if ((thisContent.indexOf(keyWords[j]))>-1){
		    	type = keyWords[j];
		    	containKey = true;
		    }
		}
		
		if (!containKey)
			type = "other";		
		return type;
	}

	public static List<String[]> AddAverage(List<String[]> modelResults) {
		// TODO Auto-generated method stub
		List<String[]> temp = modelResults;
		String[] ave = new String[temp.get(0).length];
		ave[0] = "Average";
		double[][] values = new double[temp.size()][ave.length-1];
		for (int i=0; i<temp.size(); i++){
			String[] thisItem = temp.get(i);
			for (int j=1; j<thisItem.length; j++){
				double thisValue = Double.parseDouble(thisItem[j]);
				values[i][j-1] = thisValue;
			}
		}
		
		List<List<Double>> listValues = new ArrayList<List<Double>>();
		for (int j=0; j<(ave.length-1); j++){
			List<Double> thisColumn = new ArrayList<Double>();
			for (int i=0; i<(temp.size()); i++){
			 double thisValue = values[i][j];
			 thisColumn.add(thisValue);
			}
			double thisAve = thisColumn.stream().collect(Collectors.averagingDouble(m -> m));
			ave[j+1] = Double.toString(thisAve);
		}
		
		temp.add(ave);
		
		return temp;
	}
	
	public static String StringVectorToString (String[] vec) {
		String ret = "";
		
		for  (int i=0; i<vec.length; i++) {
			String thisStr = vec[i];
			ret = ret + thisStr + " ";
		}	
		return ret;	
	}

	public static String DoubleVectorToString(List<Double> line) {
		// TODO Auto-generated method stub
		String ret = "";
		DecimalFormat df = new DecimalFormat("0.00");	
		for (int i=0; i<line.size(); i++) {
			String thisEntropy = df.format(line.get(i).doubleValue()).toString();
			ret = ret + thisEntropy + " ";
		}
		return ret;
	}
	
	public static String StringListToString(List<String> thisLine_token) {
		// TODO Auto-generated method stub
		String ret = "";
		
		for  (int i=0; i<thisLine_token.size(); i++) {
			String thisStr = thisLine_token.get(i);
			ret = ret + thisStr + " ";
		}	
		return ret;	
	}

	public static void SubTrainSet(String trainSetPathJava, String trainSetPathCSV, String trainJavaNewUrl, String trainCsvNewUrl, int buggy_size) throws IOException {
		// TODO Auto-generated method stub
		Random rand = new Random();
	   List<String> trainLinesContentOrignial = FileUtil.readLinesFromFile(trainSetPathJava);
	   List<String[]> trainLinesOrignial = myCSV.getContentFromFile(new File(trainSetPathCSV));
	   
       List<String> trainLinesContent = new ArrayList<String>();
       List<String[]> trainLines = new ArrayList<String[]>();
       
       String[] header = myCSV.getHeaderFromFile(new File(trainSetPathCSV));
       
       
       for (int i=0; i<buggy_size; i++) {
    	 int original_size = trainLinesOrignial.size();
    	 int include_index = rand.nextInt(original_size);
    	 
    	 trainLinesContent.add(trainLinesContentOrignial.get(include_index));
    	 trainLines.add(trainLinesOrignial.get(include_index)); 
    	
    	 trainLinesContentOrignial.remove(include_index);
    	 trainLinesOrignial.remove(include_index);
    	 
       }
       	
		FileUtil.writeLinesToFile(trainLinesContent, trainJavaNewUrl);
		myCSV.writeToCsv(new File(trainCsvNewUrl), header, trainLines);	
	}
}
