package zju.defect.locater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import zju.defect.util.FileUtil;

public class PreprocessCrossProject {

	public static void Run(String modelType, String trainSourceType) throws IOException {
		// TODO Auto-generated method stub		
		String[] header = new String[]{""};
		for (int i = 0; i < DefectLocater.projects.length; i++) {
			String targetProject = DefectLocater.projects[i];
			String trainSetPathCSV = DefectLocater.root +  targetProject + "//" + modelType
					+ "//" + targetProject + "_train" + modelType + ".csv";
			String trainSetPathJava = DefectLocater.root +  targetProject + "//" + modelType
					+ "//" + targetProject + "_train" + modelType + ".java";
			List<String[]> combinedTrainsetCSV = new ArrayList<String[]>();
			List<String> combinedTrainsetJava = new ArrayList<String>();
			for (int j = 0; j < DefectLocater.projects.length; j++) {
				if (i != j) {
					String sourceProject = DefectLocater.projects[j];	
					
					String sourcePathCSV = DefectLocater.root +  sourceProject + "//" + trainSourceType
							+ "//" + sourceProject + "_train" + trainSourceType + ".csv";
					String sourcePathJava = DefectLocater.root +  sourceProject + "//" + trainSourceType
							+ "//" + sourceProject + "_train" + trainSourceType + ".java";
					
					List<String[]> trainCSVContent = DefectLocater.myCSV.getContentFromFile(new File(sourcePathCSV));
					List<String> trainJavaContent = FileUtil.readLinesFromFile(sourcePathJava);

					header = DefectLocater.myCSV.getHeaderFromFile(new File(sourcePathCSV));

					combinedTrainsetCSV = FileUtil.CombineList(combinedTrainsetCSV, trainCSVContent);
					combinedTrainsetJava = FileUtil.CombineListString(combinedTrainsetJava, trainJavaContent);
				}
			}			
			DefectLocater.myCSV.writeToCsv(new File(trainSetPathCSV), header, combinedTrainsetCSV);
			FileUtil.writeLinesToFile(combinedTrainsetJava, trainSetPathJava);
		}				
	}
}
