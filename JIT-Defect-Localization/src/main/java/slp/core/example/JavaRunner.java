package slp.core.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.CLI;
import slp.core.counting.giga.GigaCounter;
import slp.core.io.Writer;
import slp.core.lexing.LexerRunner;
import slp.core.lexing.code.JavaLexer;
import slp.core.modeling.Model;
import slp.core.modeling.ModelRunner;
import slp.core.modeling.mix.InverseMixModel;
import slp.core.modeling.mix.NestedModel;
import slp.core.modeling.ngram.ADMModel;
import slp.core.modeling.ngram.ADModel;
import slp.core.modeling.ngram.JMModel;
import slp.core.modeling.ngram.NGramCache;
import slp.core.modeling.ngram.NGramModel;
import slp.core.modeling.ngram.WBModel;
import slp.core.translating.Vocabulary;
import slp.core.util.Pair;
import zju.defect.locater.DefectLocater;
import zju.defect.locater.ThresholdLearning;
import zju.defect.util.CSV_handler;
import zju.defect.util.FileUtil;

public class JavaRunner {

	private static String trainPathJava;
	private static String trainPathCSV;
	private static String testPathCSV;
	private static String testPathJava;
	private static String valPathCSV;
	private static String valPathJava;
	private static String entropyTrainResultPath;
	private static String entropyValResultPath;
	private static String entropyTestResultPath;
	private static int N_gram;
	private static CSV_handler myCSV = new CSV_handler();
	
	private static double threshold = 0;

	private static boolean modelPerLine = true;
	private static boolean sentenceMarker = true;

	
	public void Initilation(String trainPathJavaProject, String trainPathCSVProject, String testPathJavaProject, String testPathCSVProject,
			  String entropyTestResultPathProject, int N_gramLength) {
		trainPathJava = trainPathJavaProject;
		trainPathCSV = trainPathCSVProject;
		testPathJava = testPathJavaProject;
		testPathCSV = testPathCSVProject;
		entropyTestResultPath = entropyTestResultPathProject;
		N_gram = N_gramLength; 
	}

	public JavaRunner() {
		// TODO Auto-generated constructor stub
	}

	public void LocateModeling(String modelType, String ngramType) throws IOException {

		// if (args.length < 1) return;
		// Assumes at least one argument, the path (file or directory) to train
		// on
		// File train = new File(args[0]);
		File train = new File(trainPathJava);
		File test = new File(testPathJava);
		// If second argument, will test on that path, else will 'self-test' on
		// train using full cross-validation per line
		// File test = args.length < 2 ? train : new File(args[1]);
		//File test = new File(testPathJava);
		// 1. Lexing
		// a. Set up lexer using a JavaLexer
		LexerRunner.setLexer(new JavaLexer());
		// b. Do not tokenize per line for Java (invoked for the sake of
		// example; false is the default)
		//LexerRunner.perLine(false);
		LexerRunner.perLine(modelPerLine);
		// c. But, do add sentence markers (to start and end of file)
		LexerRunner.addSentenceMarkers(sentenceMarker);
		// d. Only lex (and thus implicitly, model) files ending with "java".
		// See also 'useRegex'
		LexerRunner.useExtension("java");
		// 2. Vocabulary:
		// - Since we are not modifying the defaults in any way, we'll just let
		// the vocabulary be built while training.
		// - Building it first using the default settings (no cut-off, don't
		// close after building)
		// should yield the same result, but could e.g. allow one to write the
		// vocabulary before training.
		// - If interested, use: VocabularyRunner.build(train);
		// 3. Model
		// a. No per line modeling for Java (default)
		// ModelRunner.perLine(false);
		ModelRunner.perLine(modelPerLine);
		// b. Self-testing if train is equal to test; will un-count each file
		// before modeling it.
		// ModelRunner.selfTesting(train.equals(test));
		//ModelRunner.selfTesting(true);
		// c. Set n-gram model order, 6 works well for Java
		ModelRunner.setNGramOrder(N_gram);
		// d. Use an n-gram model with simple Jelinek-Mercer smoothing (works
		// well for code)
		// - Let's use a GigaCounter, fit for large corpora here as well; the
		// nested model later on will copy this behavior.
		Model model = new JMModel(new GigaCounter());
		if (ngramType.equals("AD"))
	       model = new ADModel(new GigaCounter());
	    else if (ngramType.equals("ADM"))
	       model = new ADMModel(new GigaCounter());
	    else if (ngramType.equals("JM"))
	       model = new JMModel(new GigaCounter());
	    else if (ngramType.equals("WB"))
	       model = new WBModel(new GigaCounter());
	    else
	      System.out.print("Not found!");
		//Model model = new NGramCache();
		//Model model = new JMModel();
		// e. First, train this model on all files in 'train' recursively, using
		// the usual updating mechanism (same as for dynamic updating).
		// - Note that this invokes Model.learn for each file, which is fine for
		// n-gram models since these are count-based;
		// other models may prefer to pre-train when calling the Model's
		// constructor.
		ModelRunner.learn(model, train);
		
		System.out.println(trainPathJava+"  Vocabualry:"+Vocabulary.size());
		// f. To get more fancy for source code, we can convert the model into a
		// complex mix model.
		// - First wrap in a nested model, causing it to learn all files in test
		// into a new 'local' model
		 //model = new NestedModel(test, model);
		// - Then, add an ngram-cache component.
		// * Note, order matters! The most local model should be "right-most" so
		// it is called upon last (i.e. "gets the final say")
		// * This is also why we apply the cache after the nested model
		 //model = new InverseMixModel(model, new NGramCache());
		// - Finally, enable dynamic updating for the whole mixture (won't
		// affect cache; it's dynamic by default)
		//model.setDynamic(true);
		if (modelType.equals("CleanNgramDynamic") || modelType.equals("BugAddNgramDynamic")) {
			model.setDynamic(true);
		}
		// 4. Running
		// a. Model each file in 'test' recursively
		
		//List<Pair<File, List<List<Double>>>> modeledFilesList = modeledFiles.collect(Collectors.toList());
		//threshold = ModelTuning(model, bugNgram);			
		ModelTesting(model, modelType);
		//ModelTestingOnTrain(model, modelType);
		// b. Retrieve entropy statistics by mapping the entropies per file
		// DoubleSummaryStatistics statistics = modeledFiles.map(pair ->
		// pair.right)
		// Note the "skip(1)" (per file), since we added delimiters and we
		// generally don't model the start-of-line token
		// .flatMap(f -> f.stream().flatMap(l -> l.stream()).skip(1))
		// .mapToDouble(d -> d)
		// .summaryStatistics();
		// System.out.printf("Modeled %d tokens, average entropy:\t%.4f\n",
		// statistics.getCount(), statistics.getAverage());
	}

	private void ModelTesting(Model model, String modelType) throws IOException {
		// TODO Auto-generated method stub
		File test = new File(testPathJava);
		Stream<Pair<File, List<List<Double>>>> modeledFiles = ModelRunner.model(model, test);
		List<Pair<File, List<List<Double>>>> modeledFilesList = modeledFiles.collect(Collectors.toList());
		GenerateEntropy(modeledFilesList, testPathCSV, entropyTestResultPath, modelType);
	}

	public void GenerateEntropy(List<Pair<File, List<List<Double>>>> modeledFilesList, String inputTestOrValCSV, String entropyResultPath, String modelType) throws IOException {
		// List<String> vocabularyWords = Vocabulary.getWords();
		// Map<String, Integer> vocabularyIndices = Vocabulary.getWordIndices();
		// List<Integer> vocabularyCounts = Vocabulary.getCounts();
		List<String[]> testSet = myCSV.getContentFromFile(new File(inputTestOrValCSV));
		List<String[]> result = new ArrayList<String[]>();
		String[] headers = myCSV.getHeaderFromFile(new File(inputTestOrValCSV));

		for (int index = 0; index < modeledFilesList.size(); index++) {
			//File resultFile = new File("D://result.csv");
			Pair<File, List<List<Double>>> thisPair = modeledFilesList.get(index);
			Pair<File, List<List<Double>>> newP = Writer.writeEntropies(thisPair);
			List<List<String>> tokens = LexerRunner.lex(thisPair.left).map(l -> l.collect(Collectors.toList()))
					.collect(Collectors.toList());
			// CLI.logger.append(p.left.getAbsolutePath());
			// CLI.logger.append('\n');
			// String thisLine="";
			List<List<Double>> right = thisPair.right;
			if (testSet.size() > 0) {
				for (int i = 0; i < right.size() && i<testSet.size(); i++) {
				//for (int i = 0; i < right.size(); i++) {
					String[] thisLine = testSet.get(i);
                    List<String> thisLine_token = tokens.get(i);
					List<Double> line = right.get(i);					
					double averageEntropy = 0;
					if (i > 0) {
						if (modelType.indexOf("Clean") > -1 && line.size()>0) {
							//System.out.println("bbbbbbb:"+line.size());
							double max = line.stream().max(Comparator.comparing(u -> u)).get();
							double ave = line.stream().collect(Collectors.averagingDouble(m -> m));
							if (modelType.equals("CleanNgram"))
								averageEntropy = max + ave;
							else if (modelType.equals("CleanNgramAve"))
								averageEntropy = ave;
							else if (modelType.equals("CleanNgramMax"))
								averageEntropy = max;
							else
								System.out.println("Bugbugbug!");					
						} 
					} else {
						if (modelType.indexOf("Clean") > -1) {
							double max = line.stream().skip(1).max(Comparator.comparing(u -> u)).get();
							double ave = line.stream().collect(Collectors.averagingDouble(m -> m));
							//averageEntropy = max + ave;
							//averageEntropy = max;
							if (modelType.equals("CleanNgram"))
								averageEntropy = max + ave;
							else if (modelType.equals("CleanNgramAve"))
								averageEntropy = ave;
							else if (modelType.equals("CleanNgramMax"))
								averageEntropy = max;
							else
								System.out.println("Bugbugbug!");
						} 
					}
					String[] entropy = new String[] { Double.toString(averageEntropy) };
					String[] newThisLine = FileUtil.combineStringVector(thisLine, entropy);
					result.add(newThisLine);
				}
			}

			List<String[]> sortedEntropyResult = result.stream().sorted((e1, e2) -> {
				return (e1[0].compareTo(e2[0]));
			}).collect(Collectors.toList());			
			myCSV.writeToCsv(new File(entropyResultPath), headers, sortedEntropyResult);
		}
	}	

	public static double GetThreshold(){
		return threshold;
	}
	
	public static void main(String[] args) throws IOException {

	}
}
