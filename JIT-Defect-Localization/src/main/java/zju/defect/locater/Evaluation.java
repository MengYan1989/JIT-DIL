package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import zju.defect.util.CSV_handler;

public class Evaluation {

	private static CSV_handler myCSV = new CSV_handler();

	// input: entropyResultCSV, output: evaCSV
	public static Map<String, Double> EvaluateAndOutputEachCommitResult(String resultCSV, String evaCSVEachCommit,
			 String modelType, String evaCSVEachTrueBugCommit) throws IOException {
		String[] evaHeadersForCommits = new String[] { "commit_hash", "total_lines", "bug_lines", "time", "cost"};
		List<String[]> evaResult = new ArrayList<String[]>();
		List<String[]> resultEntropy = myCSV.getContentFromFile(new File(resultCSV));		
		Map<String, Double> evaluateByEntropy = EvaluateF1HitCommitsAndAsClassifier(resultEntropy, modelType, evaCSVEachTrueBugCommit);
		//evaluateByEntropy.put("EntropyThreshold", threshold);
		List<String[]> tempOneCommit = new ArrayList<String[]>();
		String commit_hash = "";
		int lines = 0;
		int bugLines = 0;
		String[] tempCommit = new String[evaHeadersForCommits.length];

		for (int i = 0; i < resultEntropy.size(); i++) {
		//for (int i=0; i<100; i++){
			String[] thisLine = resultEntropy.get(i);
			String tempCommitHash = thisLine[0];
			String time = thisLine[6];
			if (commit_hash.equals("")) {
				commit_hash = tempCommitHash;
			}

			if (tempCommitHash.equals(commit_hash) && i < resultEntropy.size() - 1) {
				tempOneCommit.add(thisLine);
				lines++;
				if (thisLine[7].toLowerCase().equals("true")) {
					bugLines++;					
				}
			} else {
				if (i == resultEntropy.size() - 1) {
					tempOneCommit.add(thisLine);
					lines++;
					if (thisLine[7].toLowerCase().equals("true"))
						bugLines++;
				}
				
				Map<String, Double> costResults = ComputeCostByCommitByRatio(tempOneCommit, bugLines, modelType);
				//double[] classifyEva = ComputeClassifyEva(tempOneCommit, bugLines, threshold, bugNgram);
				tempCommit[0] = commit_hash;
				tempCommit[1] = Integer.toString(lines);
				tempCommit[2] = Integer.toString(bugLines);
				tempCommit[3] = time;
				tempCommit[4] = Double.toString(costResults.get("recall"));
				//tempCommit[5] = Double.toString(classifyEva[0]);
				//tempCommit[6] = Double.toString(classifyEva[1]);
				//tempCommit[7] = Double.toString(classifyEva[2]);
				evaResult.add(tempCommit);
				commit_hash = "";
				lines = 0;
				bugLines = 0;
				tempOneCommit = new ArrayList<String[]>();
				tempCommit = new String[evaHeadersForCommits.length];
				if (i < resultEntropy.size() - 1)
					i--;
			}
		}
		myCSV.writeToCsv(new File(evaCSVEachCommit), evaHeadersForCommits, evaResult);
		return evaluateByEntropy;
	}

	// evaluate cost-effectiveness of a commit
	public static Map<String, Double> ComputeCostByCommitByRatio(List<String[]> tempOneCommit, int bugLines,
			String modelType) {
		// TODO Auto-generated method stub
		Map<String, Double> costResults = new HashMap<String, Double>();
		List<String[]> sortedCommit = tempOneCommit.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[10]))).compareTo(new Double(Double.parseDouble(e2[10])));
		}).collect(Collectors.toList());  // entropy sorting
		double pre = 0;
		double recall = 0;
		double top1 =0;
		double top2 = 0;
		double top5 = 0;
		double top10 = 0;
		double mr2 = 0;
		double map = 0;
		double aucec5=0;
		double aucec20 = 0;
		double aucec50 = 0;
		Map<Integer, Double> costArea = new HashMap<Integer, Double>(); // e.g.,k=20, means AUCEC_20

		int[] bugRank = new int[bugLines];
		// double threshold = 0.2;
		double hitInspected = 0;
		int hit = 0;
		
		Map<Integer, Double> linesCost = new HashMap<Integer, Double>();
		if (bugLines > 0) {
			if (modelType.equals("BugAddNgram") || modelType.equals("BugDelNgram") || modelType.equals("AllBuggyCP") || modelType.equals("CombineCleanBugAddLocalThree")) {
				for (int i = 0; i < sortedCommit.size(); i++) {
					String[] thisLine = sortedCommit.get(i);  // begin by small
					//String[] thisLine = sortedCommit.get(sortedCommit.size() - 1 - i);  // begin by big
					String entropy = thisLine[10];
					//System.out.println(entropy);
					String label = thisLine[7];
					if (label.toLowerCase().equals("true")) {
						bugRank[hit] = i + 1;
						hit++;
						if (i == 0) {
							top1 = 1;
							top2 = 1;
							top5 = 1;
							top10 = 1;
						}
						if (i == 1) {
							top2 = 1;
							top5 = 1;
							top10 =1 ;
						}
						if (i > 1 && i < 5) {
							top5 = 1;
							top10 = 1;
						}
						
						if (i>=5 && i <10)
							top10 = 1;

						//if (i < inspectedLines)
					    hitInspected = hitInspected + 1;
					   
					}
					 linesCost.put(i+1, hitInspected/bugLines);
				}
			} else {
				for (int i = 0; i < sortedCommit.size(); i++) {
					String[] thisLine = sortedCommit.get(sortedCommit.size() - 1 - i);
					String entropy = thisLine[10];
					//System.out.println("------commit:"+thisLine[0]+"  value:"+entropy);				
					String label = thisLine[7];
					if (label.toLowerCase().equals("true")) {
						bugRank[hit] = i + 1;
						hit++;
						if (i == 0) {
							top1 = 1;
							top2 = 1;
							top5 = 1;
							top10 = 1;
						}
						if (i == 1) {
							top2 = 1;
							top5 = 1;
							top10 = 1;
						}
						if (i > 1 && i < 5){
							top5 = 1;
							top10 = 1;
						}
						if (i>=5 && i <10)
							top10 = 1;
						//if (i < inspectedLines)
						hitInspected = hitInspected + 1;
						
					}
					linesCost.put(i+1, hitInspected/bugLines);
				}
			}
		}
		if(bugLines > 0){
		  costArea = GenerateAucecByLinesCost(linesCost);
		  aucec5 = costArea.get(5);
		  aucec20 = costArea.get(20);
		  aucec50 = costArea.get(50);
		}
/*		if (inspectedLines > 0)
			pre = hitInspected / inspectedLines;
		if (bugLines > 0)
			recall = hitInspected / bugLines;*/

		if (bugRank.length > 0) {
			mr2 = 1.0 / bugRank[0];
			//mr2 = GetMRRByBugRank(bugRank, bugLines);
			map = GetMapByBugRank(bugRank, bugLines);
			//map = GetGMapByBugRank(bugRank, bugLines);
		}

		costResults.put("pre", pre);
		costResults.put("recall", recall);
		costResults.put("mr2", mr2);
		costResults.put("map", map);
		costResults.put("top1", top1);
		costResults.put("top2", top2);
		costResults.put("top5", top5);
		costResults.put("top10", top10);
		costResults.put("aucec5", aucec5);
		costResults.put("aucec20", aucec20);
		costResults.put("aucec50", aucec50);
		
		return costResults;
	}

	static double GetMRRByBugRank(int[] bugRank, int bugLines) {
		// TODO Auto-generated method stub
		double mrr = 0;
		for (int i = 0; i < bugRank.length; i++) {
			int thisRank = bugRank[i];
			mrr = mrr + (1.0 / thisRank);
		}
		return mrr / bugRank.length;
	}

	static Map<Integer, Double> GenerateAucecByLinesCost(Map<Integer, Double> linesCost) {
		//Map<Integer, Double> costAreaAtEachPoint = new HashMap<Integer, Double>();
		Map<Integer, Double> costAreaAdded = new HashMap<Integer, Double>();
		Map<Integer, Double> ratioCost = new HashMap<Integer, Double>();
		double addedArea = 0;
		int lines = linesCost.size();
		for (int i=1; i<=100; i++){
			double thisRatio = i/100.00;
			int thisRatioLines = getInt (thisRatio*lines);
			double thisRatioCost = 0;
			if (thisRatioLines > 0)
			   thisRatioCost = linesCost.get(thisRatioLines);	
			
			ratioCost.put(i, thisRatioCost);
			double thisRatioArea = 0; 
			if (i==1){
				thisRatioArea = thisRatioCost*0.5*0.01;
				//costAreaAtEachPoint.put(i, thisRatioArea);
			}
			else{
				double previousRatioCost = ratioCost.get(i-1);				
				thisRatioArea = (previousRatioCost + thisRatioCost)*0.5*0.01;
			}
			addedArea = addedArea + thisRatioArea;
			//costAreaAtEachPoint.put(i, thisRatioArea);
			costAreaAdded.put(i, addedArea);
		}		
		return costAreaAdded;
	}

    public static int getInt(double number){
       BigDecimal bd=new BigDecimal(number).setScale(0, BigDecimal.ROUND_HALF_UP);
       return Integer.parseInt(bd.toString()); 
    } 
	
	public static double GetMapByBugRank(int[] bugRank, int bugLines) {
		// TODO Auto-generated method stub
		double map = 0;
		for (int i = 0; i < bugRank.length; i++) {
			int thisRank = bugRank[i];
			map = map + ((i + 1.0) / thisRank);
		}
		return map / bugRank.length;
	}

	
	public static double GetGMapByBugRank(int[] bugRank, int bugLines) {
		// TODO Auto-generated method stub
		double map = 0;
		for (int i = 0; i < bugRank.length; i++) {
			int thisRank = bugRank[i];
			map = map + Math.log(((i + 1.0) / thisRank));
		}
		return Math.exp(map / bugRank.length);
	}

	public static Map<String, Double> EvaluateF1HitCommitsAndAsClassifier(List<String[]> resultEntropy,
		 String modelType, String trueBugTestResultPath) throws IOException {
		// TODO Auto-generated method stub
		String[] evaHeaders = new String[] { "commit_hash", "total_lines", "bug_lines", "mr2Cost", "mapCost", "top1", "top2", "top5", "top10", "aucec5", "aucec20", "aucec50"};
		List<String[]> evaResultForTP = new ArrayList<String[]>();
		List<String[]> evaResultForTN = new ArrayList<String[]>();
		List<String[]> evaResultForTrueBugs = new ArrayList<String[]>(); // bug_line																			// >																		// 0;
		Map<String, Double> typePerformance = new HashMap<String, Double>();
		List<String[]> tempOneCommit = new ArrayList<String[]>();
		String commit_hash = "";
		int lines = 0;
		int bugLines = 0;
		String[] tempCommit = new String[evaHeaders.length];
		double hitBug = 0;
		double hitClean = 0;
		double allBug = 0;
		double allClean = 0;
		double allCommits = 0;
		for (int i = 0; i < resultEntropy.size(); i++) {
			String[] thisLine = resultEntropy.get(i);
			String tempCommitHash = thisLine[0];
			if (commit_hash.equals("")) {
				commit_hash = tempCommitHash;
			}
			if (tempCommitHash.equals(commit_hash) && i < resultEntropy.size() - 1) {
				tempOneCommit.add(thisLine);
				lines++;
				if (thisLine[7].toLowerCase().equals("true"))
					bugLines++;
			} else {
				if (i == resultEntropy.size() - 1) {
					tempOneCommit.add(thisLine);
					lines++;
					if (thisLine[7].toLowerCase().equals("true"))
						bugLines++;
				}
				allCommits++;
				String commitLabel = tempOneCommit.get(0)[8].toLowerCase(); 			
				Map<String, Double> costResults = ComputeCostByCommitByRatio(tempOneCommit, bugLines, modelType);
				tempCommit[0] = commit_hash;
				tempCommit[1] = Integer.toString(lines);
				tempCommit[2] = Integer.toString(bugLines);
				//tempCommit[3] = Double.toString(classifyEva[0]);
				//tempCommit[4] = Double.toString(classifyEva[1]);
				//tempCommit[5] = Double.toString(classifyEva[2]);
				//tempCommit[3] = Double.toString(costResults.get("pre"));
				//tempCommit[4] = Double.toString(costResults.get("recall"));
				tempCommit[3] = Double.toString(costResults.get("mr2"));
				tempCommit[4] = Double.toString(costResults.get("map"));
				tempCommit[5] = Double.toString(costResults.get("top1"));
				tempCommit[6] = Double.toString(costResults.get("top2"));
				tempCommit[7] = Double.toString(costResults.get("top5"));
				tempCommit[8] = Double.toString(costResults.get("top10"));
				tempCommit[9] = Double.toString(costResults.get("aucec5"));
				tempCommit[10] = Double.toString(costResults.get("aucec20"));
				tempCommit[11] = Double.toString(costResults.get("aucec50"));
				
				if (commitLabel.equals("true")) {
					hitBug++;					
					if (bugLines > 0) {//  filter targets
						allBug++;
						evaResultForTP.add(tempCommit);
						evaResultForTrueBugs.add(tempCommit);
					}
					else{
						allClean++;
					}
				}
				else{
					allClean++;
				}
				tempOneCommit = new ArrayList<String[]>();
				tempCommit = new String[evaHeaders.length];
				commit_hash = "";
				lines = 0;
				bugLines = 0;
				if (i < resultEntropy.size() - 1)
					i--;
			}
		}
		System.out.println(trueBugTestResultPath + " Number of changes:" + evaResultForTrueBugs.size());
		myCSV.writeToCsv(new File(trueBugTestResultPath), evaHeaders, evaResultForTrueBugs);
		
		List<Double> allF1Bug = evaResultForTP.stream().map(i -> Double.parseDouble(i[5])).collect(Collectors.toList());
		List<Double> allF1Clean = evaResultForTN.stream().map(i -> Double.parseDouble(i[5]))
				.collect(Collectors.toList());

		//List<Double> allPreTrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[6]))
		//		.collect(Collectors.toList());
		//List<Double> allRecallTrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[7]))
		//		.collect(Collectors.toList());
		List<Double> allMr2TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[3]))
				.collect(Collectors.toList());
		List<Double> allMapTrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[4]))
				.collect(Collectors.toList());
		
		List<Double> allTop1TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[5]))
				.collect(Collectors.toList());
		List<Double> allTop2TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[6]))
				.collect(Collectors.toList());
		List<Double> allTop5TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[7]))
				.collect(Collectors.toList());
		List<Double> allTop10TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[8]))
				.collect(Collectors.toList());
		List<Double> allAucec5TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[9]))
				.collect(Collectors.toList());
		List<Double> allAucec20TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[10]))
				.collect(Collectors.toList());
		List<Double> allAucec50TrueBug = evaResultForTrueBugs.stream().map(i -> Double.parseDouble(i[11]))
				.collect(Collectors.toList());

		double sumF1HitBug = allF1Bug.stream().collect(Collectors.summingDouble(i -> i));
		//double aveF1AllCommits = (sumF1HitBug + allF1Clean.size()) / allCommits;

		//double hitBugAveF1Value = allF1Bug.stream().collect(Collectors.averagingDouble(i -> i));
		//double hitCleanAveF1Value = allF1Clean.stream().collect(Collectors.averagingDouble(i -> i));

		//double avePreAllTrueBugs = allPreTrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		//double aveRecallAllTrueBugs = allRecallTrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveMr2AllTrueBugs = allMr2TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveMapAllTrueBugs = allMapTrueBug.stream().collect(Collectors.averagingDouble(i -> i));

		double aveAllTop1TrueBug = allTop1TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop2TrueBug = allTop2TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop5TrueBug = allTop5TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllTop10TrueBug = allTop10TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec5TrueBug = allAucec5TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec20TrueBug = allAucec20TrueBug.stream().collect(Collectors.averagingDouble(i -> i));
		double aveAllAucec50TrueBug = allAucec50TrueBug.stream().collect(Collectors.averagingDouble(i -> i));


		String preClassifier = "preClassifier";
		String recallClassifier = "recallClassifier";
		String f1Classifier = "f1Classifier";
		String hitBugAveF1 = "hitBugAveF1";
		String hitBugCommits = "hitBugCommits";
		String hitCleanAveF1 = "hitCleanAveF1";
		String hitCleanCommits = "hitCleanCommits";
		String allCommitsAveF1 = "aveF1AllCommits";
		String allCommitsCount = "allCommits";
		String allBugCommitsCount = "allBugCommits";
		String allCleanCommitsCount = "allCleanCommits";
		String aveRecallTrueBugs = "aveRecallTrueBugs";
		String avePreTrueBugs = "avePreTrueBugs";
		String aveMr2TrueBugs = "aveMr2TrueBugs";
		String aveMapTrueBugs = "aveMapTrueBugs";
		String aveTop1Accuracy = "aveTop1Accuracy";
		String aveTop2Accuracy = "aveTop2Accuracy";
		String aveTop5Accuracy = "aveTop5Accuracy";
		String aveTop10Accuracy = "aveTop10Accuracy";
		String aveAucec5Accuracy = "aveAucec5Accuracy";
		String aveAucec20Accuracy = "aveAucec20Accuracy";
		String aveAucec50Accuracy = "aveAucec50Accuracy";
		
		//typePerformance.put(preClassifier, preClassifierValue);
		//typePerformance.put(recallClassifier, recallClassifierValue);
		//typePerformance.put(f1Classifier, f1ClassifierValue);
		//typePerformance.put(hitBugAveF1, hitBugAveF1Value);
		//typePerformance.put(hitCleanAveF1, hitCleanAveF1Value);
		//typePerformance.put(hitBugCommits, hitBug);
		//typePerformance.put(hitCleanCommits, hitClean);
		//typePerformance.put(allCommitsAveF1, aveF1AllCommits);
		typePerformance.put(allCommitsCount, allCommits);
		typePerformance.put(allBugCommitsCount, allBug);
		typePerformance.put(allCleanCommitsCount, allClean);
		//typePerformance.put(aveRecallTrueBugs, aveRecallAllTrueBugs);
		//typePerformance.put(avePreTrueBugs, avePreAllTrueBugs);
		typePerformance.put(aveMr2TrueBugs, aveMr2AllTrueBugs);
		typePerformance.put(aveMapTrueBugs, aveMapAllTrueBugs);
		typePerformance.put(aveTop1Accuracy, aveAllTop1TrueBug);
		typePerformance.put(aveTop2Accuracy, aveAllTop2TrueBug);
		typePerformance.put(aveTop5Accuracy, aveAllTop5TrueBug);
		typePerformance.put(aveTop10Accuracy, aveAllTop10TrueBug);
		typePerformance.put(aveAucec5Accuracy, aveAllAucec5TrueBug);
		typePerformance.put(aveAucec20Accuracy, aveAllAucec20TrueBug);
		typePerformance.put(aveAucec50Accuracy, aveAllAucec50TrueBug);
		return typePerformance;
	}

	public static String GetClassifiyType(List<String[]> tempOneCommit, double threshold, String modelType) {
		// TODO Auto-generated method stub
		String type = "";
		String commitLabel = tempOneCommit.get(0)[8].toLowerCase(); // index 8: commit label;
		List<String[]> sortedCommit = tempOneCommit.stream().sorted((e1, e2) -> {
			return e1[9].compareTo(e2[9]); // index 9: entropy value
		}).collect(Collectors.toList());
		String[] minOrMaxEntropyCommit = new String[tempOneCommit.get(0).length];

		if (modelType.equals("BugAddNgram")|| modelType.equals("BugDelNgram")) 
			minOrMaxEntropyCommit = sortedCommit.get(0);
		else
			minOrMaxEntropyCommit = sortedCommit.get(sortedCommit.size() - 1);

		double minOrMaxEntropy = Double.parseDouble(minOrMaxEntropyCommit[9]);
		String predictCommitLable = "false";
		if (modelType.equals("BugAddNgram")|| modelType.equals("BugDelNgram")) {
			if (minOrMaxEntropy < threshold)
				predictCommitLable = "true";
		} else {
			if (minOrMaxEntropy > threshold)
				predictCommitLable = "true";
		}
		if (predictCommitLable.equals(commitLabel)) {
			if (predictCommitLable.equals("true"))
				type = "tp";
			else
				type = "tn";
		} else {
			if (predictCommitLable.equals("true"))
				type = "fp";
			else
				type = "fn";
		}
		return type;
	}

	public static String[] GenerateOutputForProject(String thisProject, Map<String, Double> projectResults,
			String[] headers) {
		// TODO Auto-generated method stub
		String[] content = new String[headers.length];

		for (int i = 0; i < headers.length; i++) {
			String key = headers[i];
			String value = "";
			if (key.equals("Project"))
				value = thisProject;
			else
				value = Double.toString(projectResults.get(key));

			content[i] = value;
		}

		return content;
	}

	public static String[] GenerateOutputForProjectCP(String sourceProject, String targetProject,
			Map<String, Double> thisCPResults, String[] headers) {
		// TODO Auto-generated method stub
		String[] content = new String[headers.length];

		for (int i = 0; i < headers.length; i++) {
			String key = headers[i];
			String value = "";
			if (key.equals("SourceProject"))
				value = sourceProject;
			else if (key.equals("TargetProject"))
				value = targetProject;
			else
				value = Double.toString(thisCPResults.get(key));

			content[i] = value;
		}

		return content;
	}
}
