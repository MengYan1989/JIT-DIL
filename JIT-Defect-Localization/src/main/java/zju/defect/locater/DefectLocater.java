package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import slp.core.example.JavaRunner;
import slp.core.lexing.code.JavaLexer;
import slp.core.modeling.Model;
import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

public class DefectLocater {

	public static String[] projects = new String[] { "projectName"}; 
	public static String root = "C://data//example//";
	public static CSV_handler myCSV = new CSV_handler();
	public static double trainRatio = 0.6;
	public static double testRatio = 1.0 - trainRatio;
	 public static String[] modelTypes = new String[]{"CleanNgram"};  // default: CleanNgram
	//public static String[] ngramTypes = new String[] {"AD", "ADM", "WB"};
	// public static String[] ngramTypes = new String[]{"JM", "AD", "ADM", "WB"};
	 public static String[] ngramTypes = new String[]{"JM"};
	// public boolean crossProject = true;
	public static Map<String, Map<String, String>> allProjectLocalPaths = new HashMap<String, Map<String, String>>();

	public static void main(String[] args) throws IOException {
        int ngramLength = 6;
		RunOurmethod(ngramLength);
	}

	private static void RunOurmethod(int ngramLength) throws IOException {
		for (int i = 0; i < modelTypes.length; i++) {
			String modelType = modelTypes[i];
			for (int j = 0; j < ngramTypes.length; j++) {
			   String ngramType = ngramTypes[j];			
			   preProcess(modelType);
			   modelBuilding(modelType, ngramType, ngramLength);
			   modelEvaluation(modelType, trainRatio, ngramType, ngramLength);				
			}
		}
	}

	/*
	 * private static void CombineNgramResult() throws IOException { // TODO
	 * Auto-generated method stub combineNgram = true; ModelBuilding(bugNgram,
	 * combineNgram); }
	 */

	private static void modelBuilding(String modelType, String ngramType, int ngramLength) throws IOException {
		int total_buggy = 0;
		int total_clean = 0;
		for (int i = 0; i < projects.length; i++) {
			String thisProject = projects[i];
			String trainSetPathJava = root +  thisProject + "//" + modelType + "//" + thisProject + "_train" + modelType
					+ ".java";
			String trainSetPathCSV = root + thisProject + "//" + modelType + "//" + thisProject + "_train" + modelType
					+ ".csv";			
			int this_clean_size = myCSV.getContentFromFile(new File(trainSetPathCSV)).size();
		    total_clean = total_clean + this_clean_size;			
			//String entropyTrainResultCSV = root + thisProject + "//" + modelType + "//" + thisProject
			//		+ "_entropyTrainResult" + modelType + ".csv";
			String testSetPathJava = root + thisProject + "//" + thisProject + "_test.java";
			String testSetPathCSV = root + thisProject + "//" + thisProject + "_test.csv";
			String entropyTestResultCSV = root + thisProject + "//" + modelType + "//" + thisProject
					+ "_entropyTestResult" + modelType + ngramType + ".csv";
			JavaRunner jr = new JavaRunner();
			jr.Initilation(trainSetPathJava, trainSetPathCSV, testSetPathJava, testSetPathCSV,
				 entropyTestResultCSV, ngramLength);
			jr.LocateModeling(modelType, ngramType);
		}		
		System.out.println("clean: "+ total_clean + "; buggy: "+total_buggy);
	}

	private static void modelEvaluation(String modelType, double trainRatio, String ngramType, int ngramLength)
			throws IOException {
		List<String[]> modelResults = new ArrayList<String[]>();
		String finalAllProjectsResults = root + "allProjectsResults" + modelType + ngramType + "_" + trainRatio + "_"
				+ ngramLength + ".csv";
		String[] headers = new String[] { "Project", "aveTop1Accuracy", "aveTop2Accuracy", "aveTop5Accuracy",
				"aveTop10Accuracy", "aveAucec5Accuracy", "aveAucec20Accuracy", "aveAucec50Accuracy", "aveMr2TrueBugs",
				"aveMapTrueBugs", "allCommits", "allBugCommits", "allCleanCommits" };
		for (int i = 0; i < projects.length; i++) {
			String thisProject = projects[i];
			String evaTestEachCommitCSV = root +  thisProject + "//" + modelType + "//" + thisProject
					+ "_eachCommitResult" + modelType + ngramType + ".csv";
			String evaCSVEachTrueBugCommit = root + thisProject + "//" + modelType + "//" + thisProject
					+ "_eachTrueBugCommitResult" + modelType + ngramType + "_" + ngramLength + ".csv";
			String entropyTestResult = root + thisProject + "//" + modelType + "//" + thisProject + "_entropyTestResult"
					+ modelType + ngramType + ".csv";						
			Map<String, Double> projectResults = new HashMap<String, Double>();	
			projectResults = Evaluation.EvaluateAndOutputEachCommitResult(entropyTestResult, evaTestEachCommitCSV,
						modelType, evaCSVEachTrueBugCommit);					
			String[] outputProjectCSV = Evaluation.GenerateOutputForProject(thisProject, projectResults, headers);
			modelResults.add(outputProjectCSV);			
		}
		modelResults = FileUtil.AddAverage(modelResults);
		myCSV.writeToCsv(new File(finalAllProjectsResults), headers, modelResults);
	}

	private static void preProcess(String modelType) throws IOException {
		for (int i = 0; i < projects.length; i++) {
			String thisProject = projects[i];	
			String inputAddLines = root +  thisProject + "//" + thisProject + "_add.csv"; // provided by raszz 			
			String generateJitFeatures = root +  thisProject + "//"+ thisProject + "_jit_features.csv";  // jit features
		
			String trainSetPathJava = root + thisProject + "//" + modelType + "//" + thisProject + "_train" + modelType
					+ ".java";
			String trainSetPathCSV = root + thisProject + "//" + modelType + "//" + thisProject + "_train" + modelType
					+ ".csv";			
			
			File file = new File(trainSetPathJava);
			if (!file.exists()) {
				file.getParentFile().mkdir();
			}

			String testSetPathCSV = root + thisProject + "//" + thisProject + "_test.csv";
			String testSetPathJava = root + thisProject + "//" + thisProject + "_test.java";

			List<String[]> jitFeaturesList = myCSV.getContentFromFile(new File(generateJitFeatures));  // sorted in commit time in descending order

			int allCommits = jitFeaturesList.size();
			int testSize = (int) Math.round(allCommits * testRatio);
			int trainSize = allCommits  - testSize;

			Set<String> trainCommits = new HashSet<String>();
			Set<String> testCommits = new HashSet<String>();

			for (int j = 0; j < allCommits; j++) {
				String[] thisCommit = jitFeaturesList.get(j);
				String thisCommitId = thisCommit[0];
				if (j < testSize)
					testCommits.add(thisCommitId);
				else {
					trainCommits.add(thisCommitId);
				}
			}

			System.out.println("Input sizes for " + thisProject + "; all Commits: " + allCommits + ";  trainsize: "
					+ trainCommits.size() + "; test Commits: " + testCommits.size());
			double validTrainSize = DataPreprocessForTrain(inputAddLines, trainSetPathJava, trainCommits,
					trainSetPathCSV, modelType);
			double validTestSize = DataPreprocessForTest(inputAddLines, testSetPathCSV, testSetPathJava, testCommits,
					modelType);
			System.out.println("Valid Sizes for " + thisProject + ";  train size: " + validTrainSize + "; test size: "
					+ validTestSize);
		}
	}

	private static Set<String> getBuggyCommits(String addLines) throws IOException {
		// TODO Auto-generated method stub
		Set<String> buggyCommitIds = new HashSet<String>();
		List<String[]> addLinesList = myCSV.getContentFromFile(new File(addLines));
		for (int i=0; i<addLinesList.size();i++) {
			String[] thisLine = addLinesList.get(i);
			String id = thisLine[0];
			String label = thisLine[8].toLowerCase();
			if (label.equals("true") && !buggyCommitIds.contains(id))
				buggyCommitIds.add(id);
		}	
		return buggyCommitIds;
	}

	private static Set<String> getFixCommits(String fixCommitsFile) throws IOException {
		// TODO Auto-generated method stub
		Set<String> fixCommits = new HashSet<String>();
		List<String[]> fixCommitsFeatures = myCSV.getContentFromFile(new File(fixCommitsFile));
		for (int i=0; i<fixCommitsFeatures.size();i++) {
			String[] thisCommit = fixCommitsFeatures.get(i);
			String id = thisCommit[0];
			fixCommits.add(id);
		}	
		return fixCommits;
	}

	private static double DataPreprocessForTest(String inputAddLines, String testSetPathCSV, String testSetPathJava,
			Set<String> testCommits, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> addLines = myCSV.getContentFromFile(new File(inputAddLines));
		String[] header = myCSV.getHeaderFromFile(new File(inputAddLines));
		String[] extendedString = new String[] { "Entropy" };
		String[] testHeader = FileUtil.combineStringVector(header, extendedString);
		return GenerateTestsetByCommits(addLines, testSetPathCSV, testSetPathJava, testHeader, testCommits, modelType);
	}

	private static double GenerateTestsetByCommits(List<String[]> addLines, String testSetPathCSV,
			String testSetPathJava, String[] testHeader, Set<String> testCommits, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String> testLineContent = new ArrayList<String>();
		List<String[]> testLines = new ArrayList<String[]>();
		// double filteredLines = 0;
		Set<String> findTestCommits = new HashSet<String>();
		double find = 0;
		int totalTestLines = 0;
			// for (int i = addLines.size() - 1; i >= 0; i--) {
			for (int i = 0; i < addLines.size(); i++) {
				String[] thisLine = addLines.get(i);
				String lineContent = thisLine[1].trim();
				String commit_hash = thisLine[0];
				if (testCommits.contains(commit_hash)) {
					if (lineContent.length() >= 10) {
						String preprocessedLineContent = FileUtil.PreprocessCode(lineContent);
						testLineContent.add(preprocessedLineContent);
						testLines.add(thisLine);
						totalTestLines++;
					}
				}
			}
		System.out.println(totalTestLines);
		myCSV.writeToCsv(new File(testSetPathCSV), testHeader, testLines);
		double validSize = FileUtil.GetCommitSizeOfMultiLines(testLines);
		FileUtil.writeLinesToFile(testLineContent, testSetPathJava);
		return validSize;
	}

	private static double DataPreprocessForTrain(String inputAddLines, String trainSetPathJava,
			Set<String> trainCommits, String trainSetPathCSV, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> addLines = myCSV.getContentFromFile(new File(inputAddLines));
		String[] header = myCSV.getHeaderFromFile(new File(inputAddLines));
		return GenerateTrainsetByCommits(addLines, trainSetPathJava, trainCommits, trainSetPathCSV, header,
					modelType);
	}

	private static double GenerateTrainsetByCommits(List<String[]> delOrAddLines, String trainSetPathJava,
			Set<String> trainCommits, String trainSetPathCSV, String[] header, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String> trainLinesContent = new ArrayList<String>();
		List<String[]> trainLines = new ArrayList<String[]>();
		for (int i = 0; i < delOrAddLines.size(); i++) {
			String[] thisLine = delOrAddLines.get(i);
			String lineContent = thisLine[1].trim();
			String commit_hash = thisLine[0];			
			String label = thisLine[7].toLowerCase();			
			if (trainCommits.contains(commit_hash)  && lineContent.length() >= 10) {  // with all train commits
					String preprocessedLineContent = FileUtil.PreprocessCode(lineContent);				
					trainLinesContent.add(preprocessedLineContent);
					trainLines.add(thisLine);
			} 
		}
		FileUtil.writeLinesToFile(trainLinesContent, trainSetPathJava);
		myCSV.writeToCsv(new File(trainSetPathCSV), header, trainLines);
		double trainValidCommits = FileUtil.GetCommitSizeOfMultiLines(trainLines);
		return trainValidCommits;
	}
}
