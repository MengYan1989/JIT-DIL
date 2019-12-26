package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import zju.defect.util.CSV_handler;

public class DataFusion {
	private static CSV_handler myCSV = new CSV_handler();
	public static void GenerateCombineNgramByOrder(String entropyTestResultCSVCombineNgram,
			String entropyTestResultCSVCleanNgram, String entropyTestResultCSVBugNgram, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> bugNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSVBugNgram));
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSVCleanNgram));
		
		List<String[]> sortedBugNgramResult = bugNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = sortedCleanNgramResult;
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultCSVBugNgram));
		
		assert (sortedBugNgramResult.size()==sortedCleanNgramResult.size());				
		
		List<String[]> tempOneCommitClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitBug = new ArrayList<String[]>();
		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();
		String commit_hash = "";
		for (int i = 0; i < sortedCleanNgramResult.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLineClean = sortedCleanNgramResult.get(i);
			String[] thisLineBug = sortedBugNgramResult.get(i);

			if ((thisLineBug[0].equals(thisLineClean[0]) && thisLineBug[1].equals(thisLineClean[1]))) {

				thisLineClean[2] = Integer.toString(i);
				thisLineBug[2] = Integer.toString(i); // index 2 to keep the
														// index i
				String tempCommitHash = thisLineClean[0];
				assert (tempCommitHash.equals(thisLineBug[0]));

				if (commit_hash.equals("")) {
					commit_hash = tempCommitHash;
				}

				if (tempCommitHash.equals(commit_hash) && i < sortedCleanNgramResult.size() - 1) {
					tempOneCommitClean.add(thisLineClean);
					tempOneCommitBug.add(thisLineBug);
				} else {
					if (i == sortedCleanNgramResult.size() - 1) {
						tempOneCommitClean.add(thisLineClean);
						tempOneCommitBug.add(thisLineBug);
					}

					// String commitLabel = thisLine[8];
					tempOneCommitCombine = CombineOrder(tempOneCommitClean, tempOneCommitBug);
					combineNgramResult = UpdatecombineNgramResultByCommit(combineNgramResult, tempOneCommitCombine);
					tempOneCommitClean = new ArrayList<String[]>();
					tempOneCommitBug = new ArrayList<String[]>();
					tempOneCommitCombine = new ArrayList<String[]>();

					commit_hash = "";
					if (i < sortedCleanNgramResult.size() - 1)
						i--;
				}
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombineNgram), header, combineNgramResult);		
	}
	
	public static void GenerateCombineNgramByValue(String entropyTestResultCSVCombineNgram,
			String entropyTestResultCSV1, String entropyTestResultCSV2, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSV1));
		List<String[]> bugNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSV2));
		
		List<String[]> sortedBugNgramResult = bugNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = new ArrayList<String[]>();
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultCSV1));
		//List<Double> bugNgramResultEntropy =  bugNgramResult.stream().map(i->Double.parseDouble(i[9])).collect(Collectors.toList());
		//double aveBugNgram = bugNgramResultEntropy.stream().collect(Collectors.averagingDouble(i -> i));

		double aveBugNgram = sortedBugNgramResult.stream().mapToDouble(i->Double.parseDouble(i[10])).summaryStatistics().getAverage();
		double aveCleanNgram = sortedCleanNgramResult.stream().mapToDouble(i->Double.parseDouble(i[10])).summaryStatistics().getAverage();		
		
		assert (sortedBugNgramResult.size()==sortedCleanNgramResult.size());
		
		for (int i=0; i<sortedBugNgramResult.size(); i++){
			String[] bugNgramEntropy = sortedBugNgramResult.get(i);
			String[] cleanNgramEntropy = sortedCleanNgramResult.get(i);
			String[] combineNgramEntropy = bugNgramEntropy;
			if ((bugNgramEntropy[0].equals(cleanNgramEntropy[0]) && bugNgramEntropy[1].equals(cleanNgramEntropy[1]))){
			double entropyBug = Double.parseDouble(bugNgramEntropy[10]);
			double entropyClean = Double.parseDouble(cleanNgramEntropy[10]);
			double entropyCombine = 0;
			if (modelType.equals("CombineCleanLocalNgram")){
			   entropyCombine = 4*((entropyClean-aveCleanNgram)/aveCleanNgram)+((entropyBug-aveBugNgram)/aveBugNgram);
			   //entropyCombine = entropyBug + entropyClean;
			}
			else if(modelType.equals("CombineCleanBugAddNgram")){
				//entropyCombine = ((entropyClean-aveCleanNgram)/aveCleanNgram)-((entropyBug-aveBugNgram)/aveBugNgram);
				entropyCombine = entropyClean-entropyBug;
			}
			combineNgramEntropy[10] = Double.toString(entropyCombine);
			combineNgramResult.add(combineNgramEntropy);
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombineNgram), header, combineNgramResult);			
	}
	
	public static void GenerateCombineNgramByCommitNorValue(String entropyTestResultCSVCombineNgram,
			String entropyTestResultCSVCleanNgram, String entropyTestResultCSVBugNgram, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> bugNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSVBugNgram));
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultCSVCleanNgram));
		
		List<String[]> sortedBugNgramResult = bugNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]).compareTo((e2[0]+e2[1])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = sortedCleanNgramResult;
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultCSVBugNgram));
		
		assert (sortedBugNgramResult.size()==sortedCleanNgramResult.size());				
		
		List<String[]> tempOneCommitClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitBug = new ArrayList<String[]>();
		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();
		String commit_hash = "";
		for (int i = 0; i < sortedCleanNgramResult.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLineClean = sortedCleanNgramResult.get(i);
			String[] thisLineBug = sortedBugNgramResult.get(i);

			if ((thisLineBug[0].equals(thisLineClean[0]) && thisLineBug[1].equals(thisLineClean[1]))) {

				thisLineClean[2] = Integer.toString(i);
				thisLineBug[2] = Integer.toString(i); // index 2 to keep the
														// index i
				String tempCommitHash = thisLineClean[0];
				assert (tempCommitHash.equals(thisLineBug[0]));

				if (commit_hash.equals("")) {
					commit_hash = tempCommitHash;
				}

				if (tempCommitHash.equals(commit_hash) && i < sortedCleanNgramResult.size() - 1) {
					tempOneCommitClean.add(thisLineClean);
					tempOneCommitBug.add(thisLineBug);
				} else {
					if (i == sortedCleanNgramResult.size() - 1) {
						tempOneCommitClean.add(thisLineClean);
						tempOneCommitBug.add(thisLineBug);
					}

					// String commitLabel = thisLine[8];
					tempOneCommitCombine = CombineValueByCommitNor(tempOneCommitClean, tempOneCommitBug, modelType);
					combineNgramResult = UpdatecombineNgramResultByCommit(combineNgramResult, tempOneCommitCombine);
					tempOneCommitClean = new ArrayList<String[]>();
					tempOneCommitBug = new ArrayList<String[]>();
					tempOneCommitCombine = new ArrayList<String[]>();

					commit_hash = "";
					if (i < sortedCleanNgramResult.size() - 1)
						i--;
				}
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombineNgram), header, combineNgramResult);		
	}

	private static List<String[]> CombineValueByCommitNor(List<String[]> tempOneCommitClean,
			List<String[]> tempOneCommitBug, String modelType) {
		// TODO Auto-generated method stub
		
	    double aveClean =  tempOneCommitClean.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getAverage();
	    double minClean = tempOneCommitClean.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getMin();
	    double maxClean = tempOneCommitClean.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getMax();
	    
	    double aveBug =  tempOneCommitBug.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getAverage();
	    double minBug = tempOneCommitBug.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getMin();
	    double maxBug = tempOneCommitBug.stream().mapToDouble(i->Double.parseDouble(i[9])).summaryStatistics().getMax();
	    
		List<String[]> tempOneCommitCombine = tempOneCommitClean;
		
		for (int i=0; i<tempOneCommitClean.size(); i++){
			String[] thisLineClean = tempOneCommitClean.get(i);
			String[] thisLineBug = tempOneCommitBug.get(i);
			String[] thisLineCombine = thisLineClean;
			double entropyBug = Double.parseDouble(thisLineBug[9]);
			double entropyClean = Double.parseDouble(thisLineClean[9]);
			
			double entropyCombine = 0;
			if (modelType.equals("CombineCleanLocalNgram")){
			  //entropyCombine = ((entropyClean-aveClean)/aveClean)+((entropyBug-aveBug)/aveBug);
			  entropyCombine = ((entropyClean-minClean)/(maxClean - minClean)+(entropyBug-minBug)/(maxBug - minBug));
			  // entropyCombine = entropyBug + entropyClean;
			}
			else if (modelType.equals("CombineCleanBugAddNgram") || modelType.equals("CombineCleanBugDelNgram")){
				if (maxClean>minClean && maxBug > minBug)
			      entropyCombine = 0.7*((entropyClean-minClean)/(maxClean - minClean))-0.3*((entropyBug-minBug)/(maxBug - minBug));
				
			}
			else{
				
			}
           
			thisLineCombine[9] = Double.toString(entropyCombine);
            tempOneCommitCombine.set(i, thisLineCombine);
		}
		return tempOneCommitCombine;
	}

	private static List<String[]> UpdatecombineNgramResultByCommit(List<String[]> combineNgramResult,
			List<String[]> tempOneCommitCombine) {
		// TODO Auto-generated method stub
		
		for (int i=0; i<tempOneCommitCombine.size(); i++){
			String[] thisLine = tempOneCommitCombine.get(i);
			String index = thisLine[2];
			combineNgramResult.set(Integer.parseInt(index), thisLine);
		}
		return combineNgramResult;
	}

	private static List<String[]> CombineOrder(List<String[]> tempOneCommitClean, List<String[]> tempOneCommitBug) {
		// TODO Auto-generated method stub
		
		List<String[]> sortedClean = tempOneCommitClean.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[10]))).compareTo(new Double(Double.parseDouble(e2[10])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedBug = tempOneCommitBug.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[10]))).compareTo(new Double(Double.parseDouble(e2[10])));
		}).collect(Collectors.toList());
		
		List<String[]> tempOneCommitCombine = sortedClean;
		
		for (int i=0; i<sortedClean.size(); i++){
			String[] thisLine = sortedClean.get(i);
			String index = thisLine[2];
            int cleanOrder = GetOrderByIndex(sortedClean, index);
            //int bugOrder = sortedBug.size()-GetOrderByIndex(sortedBug, index);
            int bugOrder = GetOrderByIndex(sortedBug, index);
            //int combineOrder = cleanOrder + bugOrder;
            int combineOrder = cleanOrder - bugOrder ;
            thisLine[10] = Integer.toString(combineOrder);
            tempOneCommitCombine.set(i, thisLine);
		}
		return tempOneCommitCombine;
	}

	private static int GetOrderByIndex(List<String[]> sortedClean, String index) {
		// TODO Auto-generated method stub
		int order = 0;
		double tempValue = 0;
		for (int i=0; i<sortedClean.size(); i++){
			String[] thisLine = sortedClean.get(i);
			String thisIndex = thisLine[2];
			double thisValue = Double.parseDouble(thisLine[9]);
			if (thisValue > tempValue)
				order++;
			if (thisIndex.equals(index)){				
				break;
			}
			tempValue = thisValue;
		}
		
		return order;
	}
	
	private static int GetOrderByIndexReverse(List<String[]> sortedClean, String index) {
		// TODO Auto-generated method stub

		
		int order = 0;
		double tempValue = 0;
		for (int i=sortedClean.size()-1; i>=0; i--){
			String[] thisLine = sortedClean.get(i);						
			String thisIndex = thisLine[2];
			double thisValue = Double.parseDouble(thisLine[9]);
			if (thisValue < tempValue)
				order++;
			if (thisIndex.equals(index)){				
				break;
			}
			tempValue = thisValue;
		}
		
		return order;
	}

	public static void GenerateCombineFourNgramByNorValue(String entropyTestResultCSVCombine,
			String entropyTestResultClean, String entropyTestResultLocalClean, String entropyTestResultBugAdd,
			String entropyTestResultBugDel, String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> bugAddNgramResult = myCSV.getContentFromFile(new File(entropyTestResultBugAdd));
		List<String[]> bugDelNgramResult = myCSV.getContentFromFile(new File(entropyTestResultBugDel));
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultClean));
		List<String[]> localCleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultLocalClean));
		
		List<String[]> sortedBugAddNgramResult = bugAddNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedBugDelNgramResult = bugDelNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedLocalCleanNgramResult = localCleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = sortedCleanNgramResult;
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultClean));
		
		//assert (sortedBugNgramResult.size()==sortedCleanNgramResult.size());				
		
		List<String[]> tempOneCommitClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitLocalClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitBugAdd = new ArrayList<String[]>();
		List<String[]> tempOneCommitBugDel = new ArrayList<String[]>();
		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();
		String commit_hash = "";
		for (int i = 0; i < sortedCleanNgramResult.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLineClean = sortedCleanNgramResult.get(i);
			String[] thisLineLocalClean = sortedLocalCleanNgramResult.get(i);
			String[] thisLineBugAdd = sortedBugAddNgramResult.get(i);
			String[] thisLineBugDel = sortedBugDelNgramResult.get(i);

			if ((thisLineClean[0].equals(thisLineLocalClean[0]) && thisLineBugAdd[1].equals(thisLineBugDel[1]))) {

				thisLineClean[2] = Integer.toString(i);
				thisLineLocalClean[2] = Integer.toString(i);
				thisLineBugAdd[2] = Integer.toString(i);
				thisLineBugDel[2] = Integer.toString(i); // index 2 to keep the
														// index i
				String tempCommitHash = thisLineClean[0];
				assert (tempCommitHash.equals(thisLineBugAdd[0]));

				if (commit_hash.equals("")) {
					commit_hash = tempCommitHash;
				}

				if (tempCommitHash.equals(commit_hash) && i < sortedCleanNgramResult.size() - 1) {
					tempOneCommitClean.add(thisLineClean);
					tempOneCommitBugAdd.add(thisLineBugAdd);
					tempOneCommitLocalClean.add(thisLineClean);
					tempOneCommitBugDel.add(thisLineBugDel);
				} else {
					if (i == sortedCleanNgramResult.size() - 1) {
						tempOneCommitClean.add(thisLineClean);
						tempOneCommitBugAdd.add(thisLineBugAdd);
						tempOneCommitLocalClean.add(thisLineClean);
						tempOneCommitBugDel.add(thisLineBugDel);
					}

					// String commitLabel = thisLine[8];
					tempOneCommitCombine = CombineValueByCommitNorValueFour (tempOneCommitClean, tempOneCommitLocalClean, tempOneCommitBugAdd, tempOneCommitBugDel, modelType);
					combineNgramResult = UpdatecombineNgramResultByCommit(combineNgramResult, tempOneCommitCombine);
					tempOneCommitClean = new ArrayList<String[]>();
					tempOneCommitBugAdd = new ArrayList<String[]>();
					tempOneCommitLocalClean = new ArrayList<String[]>();
					tempOneCommitBugDel = new ArrayList<String[]>();
					tempOneCommitCombine = new ArrayList<String[]>();

					commit_hash = "";
					if (i < sortedCleanNgramResult.size() - 1)
						i--;
				}
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombine), header, combineNgramResult);	
	}

	private static List<String[]> CombineValueByCommitNorValueFour(List<String[]> tempOneCommitClean,
			List<String[]> tempOneCommitLocalClean, List<String[]> tempOneCommitBugAdd,
			List<String[]> tempOneCommitBugDel, String modelType) {
		// TODO Auto-generated method stub
		double minClean = tempOneCommitClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxClean = tempOneCommitClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();

		double minLocalClean = tempOneCommitLocalClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxLocalClean = tempOneCommitLocalClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();
		
		double minBugAdd = tempOneCommitBugAdd.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxBugAdd = tempOneCommitBugAdd.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();

		double minBugDel = tempOneCommitBugDel.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxBugDel = tempOneCommitBugDel.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();

		List<String[]> tempOneCommitCombine = tempOneCommitClean;

		for (int i = 0; i < tempOneCommitClean.size(); i++) {
			String[] thisLineClean = tempOneCommitClean.get(i);
			String[] thisLineBugAdd = tempOneCommitBugAdd.get(i);
			String[] thisLineLocalClean = tempOneCommitClean.get(i);
			String[] thisLineBugDel = tempOneCommitBugDel.get(i);
			String[] thisLineCombine = thisLineClean;
			double entropyBugAdd = Double.parseDouble(thisLineBugAdd[9]);
			double entropyBugDel = Double.parseDouble(thisLineBugDel[9]);
			double entropyClean = Double.parseDouble(thisLineClean[9]);
			double entropyLocalClean = Double.parseDouble(thisLineLocalClean[9]);

			double entropyCombine = 0;
			if (modelType.equals("CombineFour")) {
				if (maxClean > minClean && maxBugAdd > minBugAdd)
					//entropyCombine = ((entropyClean - minClean) / (maxClean - minClean))
					//		+ ((entropyLocalClean - minLocalClean) / (maxLocalClean - minLocalClean))
					//		+ ((entropyBugAdd - minBugAdd) / (maxBugAdd - minBugAdd))
					//		+ ((entropyBugDel - minBugDel) / (maxBugDel - minBugDel));
				entropyCombine = entropyClean+entropyLocalClean	+entropyBugAdd+entropyBugDel;
				// entropyCombine = entropyClean;

			} 
			thisLineCombine[9] = Double.toString(entropyCombine);
			tempOneCommitCombine.set(i, thisLineCombine);
		}
		return tempOneCommitCombine;
	}

	public static void GenerateCombineCleanBugAddLocalThreeByNorValue(String entropyTestResultCSVCombine,
			String entropyTestResultClean, String entropyTestResultLocalClean, String entropyTestResultBugAdd,
			String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> bugAddNgramResult = myCSV.getContentFromFile(new File(entropyTestResultBugAdd));		
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultClean));
		List<String[]> localCleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultLocalClean));
		
		List<String[]> sortedBugAddNgramResult = bugAddNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedLocalCleanNgramResult = localCleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = sortedCleanNgramResult;
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultClean));
		
		//assert (sortedBugNgramResult.size()==sortedCleanNgramResult.size());				
		
		List<String[]> tempOneCommitClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitLocalClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitBugAdd = new ArrayList<String[]>();
		
		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();
		String commit_hash = "";
		for (int i = 0; i < sortedCleanNgramResult.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLineClean = sortedCleanNgramResult.get(i);
			String[] thisLineLocalClean = sortedLocalCleanNgramResult.get(i);
			String[] thisLineBugAdd = sortedBugAddNgramResult.get(i);

			if ((thisLineClean[0].equals(thisLineLocalClean[0]) && thisLineBugAdd[1].equals(thisLineLocalClean[1]))) {

				thisLineClean[2] = Integer.toString(i);
				thisLineLocalClean[2] = Integer.toString(i);
				thisLineBugAdd[2] = Integer.toString(i); // index 2 to keep the
														// index i
				String tempCommitHash = thisLineClean[0];
				assert (tempCommitHash.equals(thisLineBugAdd[0]));

				if (commit_hash.equals("")) {
					commit_hash = tempCommitHash;
				}

				if (tempCommitHash.equals(commit_hash) && i < sortedCleanNgramResult.size() - 1) {
					tempOneCommitClean.add(thisLineClean);
					tempOneCommitBugAdd.add(thisLineBugAdd);
					tempOneCommitLocalClean.add(thisLineClean);
					
				} else {
					if (i == sortedCleanNgramResult.size() - 1) {
						tempOneCommitClean.add(thisLineClean);
						tempOneCommitBugAdd.add(thisLineBugAdd);
						tempOneCommitLocalClean.add(thisLineClean);					
					}

					// String commitLabel = thisLine[8];
					tempOneCommitCombine = CombineValueByCommitNorValueThree (tempOneCommitClean, tempOneCommitLocalClean, tempOneCommitBugAdd, modelType);
					combineNgramResult = UpdatecombineNgramResultByCommit(combineNgramResult, tempOneCommitCombine);
					tempOneCommitClean = new ArrayList<String[]>();
					tempOneCommitBugAdd = new ArrayList<String[]>();
					tempOneCommitLocalClean = new ArrayList<String[]>();
					
					tempOneCommitCombine = new ArrayList<String[]>();

					commit_hash = "";
					if (i < sortedCleanNgramResult.size() - 1)
						i--;
				}
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombine), header, combineNgramResult);	
	}

	private static List<String[]> CombineValueByCommitNorValueThree(List<String[]> tempOneCommitClean,
			List<String[]> tempOneCommitLocalClean, List<String[]> tempOneCommitBugAdd, String modelType) {
		// TODO Auto-generated method stub			
		double minClean = tempOneCommitClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxClean = tempOneCommitClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();

		double minLocalClean = tempOneCommitLocalClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxLocalClean = tempOneCommitLocalClean.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();
		
		double minBugAdd = tempOneCommitBugAdd.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMin();
		double maxBugAdd = tempOneCommitBugAdd.stream().mapToDouble(i -> Double.parseDouble(i[9])).summaryStatistics()
				.getMax();

		List<String[]> tempOneCommitCombine = tempOneCommitClean;

		for (int i = 0; i < tempOneCommitClean.size(); i++) {
			String[] thisLineClean = tempOneCommitClean.get(i);
			String[] thisLineBugAdd = tempOneCommitBugAdd.get(i);
			String[] thisLineLocalClean = tempOneCommitClean.get(i);
			
			String[] thisLineCombine = thisLineClean;
			double entropyBugAdd = Double.parseDouble(thisLineBugAdd[9]);			
			double entropyClean = Double.parseDouble(thisLineClean[9]);
			double entropyLocalClean = Double.parseDouble(thisLineLocalClean[9]);

			double entropyCombine = 0;
			if (modelType.equals("CombineCleanBugAddLocalThree")) {
				if (maxClean > minClean && maxBugAdd > minBugAdd)
					entropyCombine = ((entropyClean - minClean) / (maxClean - minClean))
							+ ((entropyLocalClean - minLocalClean) / (maxLocalClean - minLocalClean))
							- ((entropyBugAdd - minBugAdd) / (maxBugAdd - minBugAdd));
				//entropyCombine = entropyClean+entropyLocalClean	+entropyBugAdd+entropyBugDel;
				// entropyCombine = entropyClean;
			} 
			thisLineCombine[9] = Double.toString(entropyCombine);
			tempOneCommitCombine.set(i, thisLineCombine);
		}
		return tempOneCommitCombine;
	}
	
	public static void GenerateCombineCleanBugAddLocalThreeByOrderThree(String entropyTestResultCSVCombine,
			String entropyTestResultClean, String entropyTestResultLocalClean, String entropyTestResultBugAdd,
			String modelType) throws IOException {
		// TODO Auto-generated method stub
		List<String[]> bugAddNgramResult = myCSV.getContentFromFile(new File(entropyTestResultBugAdd));
		List<String[]> cleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultClean));
		List<String[]> localCleanNgramResult = myCSV.getContentFromFile(new File(entropyTestResultLocalClean));
		
		List<String[]> sortedBugAddNgramResult = bugAddNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedCleanNgramResult = cleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedLocalCleanNgramResult = localCleanNgramResult.stream().sorted((e1, e2) -> {
			return ((e1[0]+e1[1]+e1[4]).compareTo((e2[0]+e2[1]+e2[4])));
		}).collect(Collectors.toList());
		
		List<String[]> combineNgramResult = sortedCleanNgramResult;
		String[] header = myCSV.getHeaderFromFile(new File(entropyTestResultBugAdd));
		
		assert (sortedBugAddNgramResult.size()==sortedCleanNgramResult.size());				
		
		List<String[]> tempOneCommitClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitLocalClean = new ArrayList<String[]>();
		List<String[]> tempOneCommitBug = new ArrayList<String[]>();
		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();
		String commit_hash = "";
		
		for (int i = 0; i < sortedCleanNgramResult.size(); i++) {
			// for (int i=0; i<30; i++){
			String[] thisLineClean = sortedCleanNgramResult.get(i);
			String[] thisLineLocalClean = sortedLocalCleanNgramResult.get(i);
			String[] thisLineBug = sortedBugAddNgramResult.get(i);

			if ((thisLineBug[0].equals(thisLineClean[0]) && thisLineBug[1].equals(thisLineClean[1]))) {
				
				thisLineLocalClean[2] = Integer.toString(i);
				thisLineClean[2] = Integer.toString(i);
				thisLineBug[2] = Integer.toString(i); // index 2 to keep the
														// index i
				String tempCommitHash = thisLineClean[0];
				assert (tempCommitHash.equals(thisLineBug[0]));

				if (commit_hash.equals("")) {
					commit_hash = tempCommitHash;
				}

				if (tempCommitHash.equals(commit_hash) && i < sortedCleanNgramResult.size() - 1) {
					tempOneCommitClean.add(thisLineClean);
					tempOneCommitBug.add(thisLineBug);
					tempOneCommitLocalClean.add(thisLineLocalClean);
				} else {
					if (i == sortedCleanNgramResult.size() - 1) {
						tempOneCommitClean.add(thisLineClean);
						tempOneCommitBug.add(thisLineBug);
						tempOneCommitLocalClean.add(thisLineLocalClean);
					}

					// String commitLabel = thisLine[8];
					tempOneCommitCombine = CombineOrderByThree(tempOneCommitClean,tempOneCommitLocalClean, tempOneCommitBug);
					combineNgramResult = UpdatecombineNgramResultByCommit(combineNgramResult, tempOneCommitCombine);
					tempOneCommitClean = new ArrayList<String[]>();
					tempOneCommitBug = new ArrayList<String[]>();
					tempOneCommitLocalClean = new ArrayList<String[]>();
					tempOneCommitCombine = new ArrayList<String[]>();

					commit_hash = "";
					if (i < sortedCleanNgramResult.size() - 1)
						i--;
				}
			}
		}		
		myCSV.writeToCsv(new File(entropyTestResultCSVCombine), header, combineNgramResult);	
	}
	
	private static List<String[]> CombineOrderByThree(List<String[]> tempOneCommitClean,
			List<String[]> tempOneCommitLocalClean, List<String[]> tempOneCommitBugAdd) {
		if (tempOneCommitClean.get(0)[0].equals("007ac4a9ddd5e10383d28978fff895eca167415d")){
			System.out.println("mmmm");
		}
		
		List<String[]> sortedClean = tempOneCommitClean.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[9]))).compareTo(new Double(Double.parseDouble(e2[9])));
		}).collect(Collectors.toList());

		List<String[]> sortedBug = tempOneCommitBugAdd.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[9]))).compareTo(new Double(Double.parseDouble(e2[9])));
		}).collect(Collectors.toList());
		
		List<String[]> sortedLocalClean = tempOneCommitLocalClean.stream().sorted((e1, e2) -> {
			return (new Double(Double.parseDouble(e1[9]))).compareTo(new Double(Double.parseDouble(e2[9])));
		}).collect(Collectors.toList());

		List<String[]> tempOneCommitCombine = new ArrayList<String[]>();

		for (int i = 0; i < sortedClean.size(); i++) {
			String[] thisLine = sortedClean.get(i);
			String[] tempLine = thisLine;
			if (thisLine[0].equals("007ac4a9ddd5e10383d28978fff895eca167415d") && thisLine[1].equals("import java.util.logging.Level;")){
				System.out.println("mmmm");
			}
			String index = thisLine[2];
			int cleanOrder = GetOrderByIndexReverse(sortedClean, index);
			// int bugOrder = sortedBug.size()-GetOrderByIndex(sortedBug,
			// index);
			int localCleanOrder = GetOrderByIndexReverse(sortedLocalClean, index);
			int bugOrder = GetOrderByIndex(sortedBug, index);
			//int combineOrder = cleanOrder + bugOrder;
			double combineOrder =  0.65*cleanOrder + 0.45*localCleanOrder +  0.15*bugOrder;
			//double combineOrder =  localCleanOrder;
			tempLine[9] = Double.toString(combineOrder);
			tempOneCommitCombine.add(tempLine);
		}
		return tempOneCommitCombine;
	}


}
