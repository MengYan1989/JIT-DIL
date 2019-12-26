package br.ufrn.raszz.util;

import java.util.*;
import java.io.*;
/*
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;/**/

import br.ufrn.raszz.model.szz.DiffHunk;
import br.ufrn.raszz.model.szz.Line;
import br.ufrn.raszz.model.szz.LineType;
import br.ufrn.raszz.model.szz.SzzFileRevision;

import java.text.*;
import java.util.regex.*;

import org.apache.log4j.*;

public abstract class FileOperationsUtil {

	private static final Logger log = Logger.getLogger(FileOperationsUtil.class); 
	private static Console c = System.console();
	public static boolean isPropertyChange = false;
	//private static String regex = "(//.*)|(/\\*.*)|(\\*.*)";
	private static String regex = "/\\*(?:.|[\\n\\r])*?\\*/";

	public static boolean isPropertyChangeOnly(ByteArrayOutputStream diff) throws Exception{
		boolean yes = false;

		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(diff.toByteArray())));
		try{
			int count = 0;
			while(br.ready()){

				String line = br.readLine().trim();
				if(count <= 5){//the first lines of the diff don't matter
					//log.info("line " + line);
					count++;
					continue;
				}

				if(line.contains("Property changes on:")){
					yes = true;
					break;
				}

				//c.readLine("analyzing line " + line);

				//only searching for modified parts
				if(isAddition(line) || isDeletion(line)){
					line = line.replace("-","");
					line = line.replace("+","");
					//c.readLine("now Im inside if");
					if(!FileOperationsUtil.isCommentOrBlankLine(line)){
						//c.readLine("not a comment nor a blank line");
						yes = false;
						break;
					}
				}
			}
		} finally {
			br.close();
		}
		//c.readLine("result: " + yes);
		return yes;
	}

	public static Date getRevisionDate(String dateToParse) {
		SimpleDateFormat sdf = DateUtils.
			getSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", null);
		Date date = null;
		try{
			date = sdf.parse(dateToParse);
		} catch (Exception e){
			e.printStackTrace();
		}
		return date;
	}

	public static boolean isAddition(String line) {

		boolean result1;
		Pattern pattern = Pattern.compile("^(\\+)");
		Matcher matcher = pattern.matcher(line.trim());
		result1 = matcher.find();

		return result1;
	}

	public static boolean isDeletion(String line) {
		boolean result1;
		Pattern pattern = Pattern.compile("^(\\-)");
		Matcher matcher = pattern.matcher(line.trim());
		result1 = matcher.find();

		return result1;
	}

    public static boolean isTestFile(ByteArrayOutputStream baous) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baous.toByteArray())));
		Pattern testCase = Pattern.compile(SzzRegex.EXTENDS_TESTCASE.getValue());
		Pattern testAnnotation = Pattern.compile(SzzRegex.TEST_ANNOTATION.getValue());
		boolean result = false;
		try{
			while(br.ready()){
				String line = br.readLine();
				if(!isCommentOrBlankLine(line)){
					Matcher m1 = testCase.matcher(line);
					Matcher m2 = testAnnotation.matcher(line);
					if(m1.find() || m2.find()){
						result = true;
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			br.close();
		}
		return result;
	}

	public static String prepareLineContent(Line line){
		String content = line.getContent().trim();
		content = removeComment(content);
		if(content.length() > 0){
			if(content.charAt(0) == '+'){
				content = content.replaceFirst("\\+","");
				content = content.trim();
			} else if (content.charAt(0) == '-'){
				content = content.replaceFirst("-","");
				content = content.trim();
			}
		}
		content = content.replace(" ","");
		if(!content.endsWith("}")){
			content = content + "}";
		}
		if(!content.startsWith("}")){
			content = "}" + content;
		}
		return content;
	}

	public static String prepareContent(String line){
		String content = line.trim();
		if(content.length() > 0){
			if(content.charAt(0) == '+'){
				content = content.replaceFirst("\\+","");
				content = content.trim();
			} else if (content.charAt(0) == '-'){
				content = content.replaceFirst("-","");
				content = content.trim();
			}
		}
		content = content.replaceAll(" ","");
		return content;
	}

	// 为了输出结果专门设置此函数
	public static String prepareContent2(String line){
		String content = line.trim();
		if(content.length() > 0){
			if(content.charAt(0) == '+'){
				content = content.replaceFirst("\\+","");
				content = content.trim();
			} else if (content.charAt(0) == '-'){
				content = content.replaceFirst("-","");
				content = content.trim();
			}
		}
		return content;
	}

	public static List<String> getHeaders(ByteArrayOutputStream diff) throws IOException {
		List<String> headers = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(diff.toByteArray())));

		try {
			while (br.ready()) {
				String line = br.readLine();
				line = line.trim();
				if (isHunkHeader(line)) {
					headers.add(line);
				}
				if(line.contains("Property changes on")){
					isPropertyChange = true;
				}
			}
		} finally {
			br.close();
		}
		return headers;
	}


	public static void joinUnfinishedLines(List<DiffHunk> hunks){
		for(DiffHunk hunk : hunks){
			List<Line> lines = hunk.getContent();
			
			joinUnfinishedLinesWhenCloning(lines, false);
		}
	}

	public static void joinUnfinishedLinesWhenCloning(List<Line> lines, boolean excludeRemovedLine){
		joinUnfinishedLinesWhenCloning(lines, excludeRemovedLine, false);
	}

	public static void joinUnfinishedLinesWhenCloning(List<Line> lines,
													  boolean excludeRemovedLine,
													  boolean doNotRemoveBlanks){
		int index = 0;		
		for(Line line : lines){
			int linenumber = (excludeRemovedLine)?line.getNumber():line.getPreviousNumber();
			if (linenumber == -1) {
				index++;
				continue;
			}			
			
			String content = line.getContent();
			content = removeComment(content);
			if (doNotRemoveBlanks)
				content = prepareContent2(content);
			else
				content = prepareContent(content);

			if(isCommentOrBlankLine(content)){
				index++;
				line.setContent(content);
				continue;
			}
			int count = 1;

			// 当前行Pmd level
			int pmdLevel = line.getPmdLevel();

			while(unfinished(content)){
				line.setContentAdjusted(true);
				//c.readLine(" it is not finished! ");
				//getting next line
				int final_index = index+count;
				if(final_index >= lines.size()){
					break;
				}
				Line nextline = lines.get(final_index);

				// 判断当前行是否可以合并
				linenumber = (excludeRemovedLine)?nextline.getNumber():nextline.getPreviousNumber();
				if (linenumber == -1) {
					count++;
					continue;
				}

				// 下一行Pmd level
				int nextPmdLevel = nextline.getPmdLevel();
				if (pmdLevel < nextPmdLevel)
					pmdLevel = nextPmdLevel;
				
				String nextcontent = nextline.getContent();
				nextcontent = removeComment(nextcontent);
				if (doNotRemoveBlanks) {
					nextcontent = prepareContent2(nextcontent);
					content = content + " " + nextcontent;
				}
				else {
					nextcontent = prepareContent(nextcontent);
					content = content + nextcontent;
				}


				line.setContent(content);
				line.setPmdLevel(pmdLevel);
				nextline.setContent(" ");
				count++;
			}
			index++;
		}
	}

	public static boolean unfinished(String content){
		boolean result = false;
		content = content.trim().replace(" ","");
		//content = removeComment(content);
		if(!content.endsWith(";") & !content.endsWith("{") &
				!content.endsWith("}") &
				!content.endsWith(":")){
			if(!isAnnotation(content)){
				result = true;
			}
		}
		return result;
	}

	public static boolean isAnnotation(String content){
		Pattern p = Pattern.compile("^\\@.*$");
		Matcher m = p.matcher(content);
		if(m.find()){
			return true;
		}
		return false;
	}

	public static boolean runRegex(String content, String regex){
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		if(m.find()){
			return true;
		}
		return false;
	}

	public static boolean isCommentOrBlankLine(String line){
		line = prepareContent(line);
		if (line.length() == 0)
			return true;

		if(line.equals("\\Nonewlineatendoffile")){
			return true;
		}

		boolean result = false;
		Pattern pattern = Pattern.compile("^(//)(.*)$");
		Matcher matcher = pattern.matcher(line.trim());

		result = matcher.find();
		if (result)
			return true;

		pattern = Pattern.compile("^(/\\*)(.*)$");
		matcher = pattern.matcher(line.trim());

		result = matcher.find();
		if (result)
			return true;

		pattern = Pattern.compile("^(\\*)(.*)$");
		matcher = pattern.matcher(line.trim());

		result = matcher.find();
		if (result)
			return true;

		pattern = Pattern.compile("^(})(\\s*)$");
		matcher = pattern.matcher(line.trim());

		result = matcher.find();
		if (result)
			return true;

		pattern = Pattern.compile("^(\\{)(\\s*)$");
		matcher = pattern.matcher(line.trim());

		result = matcher.find();
		if (result)
			return true;

		return false;
	}

	public static List<DiffHunk> getDiffHunks(ByteArrayOutputStream diff, List<String> headers, 
			String previousPath, String nextPath, String revision, String nextRevision)
			throws IOException {
		List<DiffHunk> hunks = new ArrayList<DiffHunk>();
		for (String header : headers) {
			List<Line> deletionsBuffer = new ArrayList<Line>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(diff.toByteArray())));
			try {
				//used to build the evolution relationship when a modification occurs
				boolean linkageFlag = false;
				LineType previousType = LineType.CONTEXT;
				boolean headerFound = false;

				//@@ -[9],10 +9,10 @@
				int prevRevContextStartNumber = getPrevContextStartingLineNumber(header);
				//@@ -9,10 +[9],10 @@
				int nextRevContextStartNumber = getNextContextStartingLineNumber(header);

				//@@ -9,[10] +9,10 @@
				//int prevRevContextLineRange = getPrevContextLineRange(header);
				//@@ -9,10 +9,[10] @@
				//int nextRevContextLineRange = getNextContextLineRange(header);


				//we will need this when we adjust the content of a line to infer the number of the following revision
				int context_difference = nextRevContextStartNumber - prevRevContextStartNumber; 

				int prevSync =  prevRevContextStartNumber; 
				int nextSync = nextRevContextStartNumber;
				int deletions = 0;
				int additions = 0;

				boolean firstOccurrenceFound = false; //track when we reach the first context line
				boolean startDelCount = false;
				boolean startAddCount = false;

				DiffHunk hunk = new DiffHunk();
				hunk.setHeader(header);

				while (br.ready()) {
					String line = br.readLine();

					if (!headerFound && line.trim().equals(header)) {
						headerFound = true;
						continue;
					}

					if (headerFound) {
						Line lineobj = new Line();
						lineobj.setPreviousPath(previousPath);
						lineobj.setNextPath(nextPath);
						lineobj.setContent(line);
						if (!isHunkHeader(line)) {

							if (isDeletion(line)) {
								lineobj.setAdditions(additions);
								lineobj.setDeletions(deletions);
								deletions++;
								// comming from an addition
								if (linkageFlag) {
									deletionsBuffer.clear();
								}
								lineobj.setType(LineType.DELETION);
								if(!firstOccurrenceFound){
									firstOccurrenceFound = true;     
									startDelCount = true;
								} else if (!startDelCount){  //i think it doesnt happen in practice
									startDelCount = true;
								} else {
									prevSync++;
								}

								lineobj.setPreviousNumber(prevSync);
								lineobj.setPreviousRevision(revision);
								lineobj.setContext_difference(context_difference);
								deletionsBuffer.add(lineobj);

								linkageFlag = false;
								previousType = LineType.DELETION;

							} else if (isAddition(line)) {
								lineobj.setDeletions(deletions);
								lineobj.setAdditions(additions);
								additions++;
								if (previousType == LineType.DELETION) {
									linkageFlag = true;
								}
								lineobj.setType(LineType.ADDITION);
								if(!firstOccurrenceFound){
									firstOccurrenceFound = true;     
									startAddCount = true;
								} else if (!startAddCount){ //very rare case
									startAddCount = true;
								} else {
									nextSync++;
								}
								lineobj.setNumber(nextSync);
								lineobj.setRevision(nextRevision);
								lineobj.setContext_difference(context_difference);
								previousType = LineType.ADDITION;
								if (linkageFlag) {
									lineobj.getOrigins().addAll(deletionsBuffer);

									for (Line deletion : deletionsBuffer) {
										deletion.getEvolutions().add(lineobj);
									}
								}
							} else {
								if(!firstOccurrenceFound){ 
									firstOccurrenceFound = true;     
									startDelCount = true;
									startAddCount = true;
								} else { 
									if(!startDelCount){
										startDelCount = true;
									} else {
										prevSync++;
									}	

									if(!startAddCount){
										startAddCount = true;
									} else {
										nextSync++;
									}
								}
								lineobj.setType(LineType.CONTEXT);
								lineobj.setPreviousRevision(revision);
								lineobj.setRevision(nextRevision);
								lineobj.setPreviousNumber(prevSync);

								lineobj.setNumber(nextSync);
								previousType = LineType.CONTEXT;
								linkageFlag = false;
								deletionsBuffer.clear();
							}
							//these lines don't have to be bothered anymore
							lineobj.setFoundInDiffHunks(true);
							hunk.getContent().add(lineobj);
						} else {
							break;
						}
					}
				}
				hunks.add(hunk);
			} finally {
				br.close();
			}
		}
		
		return hunks;
	}

	public static int getPrevContextStartingLineNumber(String header) {
		String[] tokens = header.split(" ");
		String toAnalyze = tokens[1];
		String[] tokens2 = toAnalyze.split(",");
		String lineNumberStr = tokens2[0].replace("-", "");
		int lineNumber = Integer.valueOf(lineNumberStr);
		return lineNumber;
	}

	public static int getPrevContextLineRange(String header) {
		String[] tokens = header.split(" ");
		String toAnalyze = tokens[1];
		String[] tokens2 = toAnalyze.split(",");
		String lineNumberStr = tokens2[1];
		int lineNumber = Integer.valueOf(lineNumberStr);
		return lineNumber;
	}

	public static int getNextContextStartingLineNumber(String header) {
		String[] tokens = header.split(" ");
		String toAnalyze = tokens[2];
		String[] tokens2 = toAnalyze.split(",");
		String lineNumberStr = tokens2[0].replace("+", "");
		int lineNumber = Integer.valueOf(lineNumberStr);
		return lineNumber;
	}

	public static int getNextContextLineRange(String header) {
		String[] tokens = header.split(" ");
		String toAnalyze = tokens[2];
		String[] tokens2 = toAnalyze.split(",");
		String lineNumberStr = tokens2[1];
		int lineNumber = Integer.valueOf(lineNumberStr);
		return lineNumber;
	}

	public static boolean isHunkHeader(String line) {
		Pattern pattern = Pattern.compile("@@\\s-\\d+,\\d+\\s\\+\\d+,\\d+\\s@@");
		Matcher matcher = pattern.matcher(line);
		return matcher.find();
	}

	public static boolean isImport(String content) {
		boolean result = false;
		content = prepareContent(content);
		Pattern pattern = Pattern.compile("^(import)(\\s*)(.*)$");
		Matcher matcher = pattern.matcher(content);
		result = matcher.find();
		if(result){
			log.info("import statement found, skipping it!");
		}
		return result;
	}	

	public static SzzFileRevision getPrevRev(SzzFileRevision fileRevision, List<SzzFileRevision> revisions){
		SzzFileRevision prev = null;
		int index = revisions.indexOf(fileRevision);
		//log.info("index " + index);
		//in case the file is not  the first of the collection
		if(index > 0){
			//log.info("prev index " + (index-1));
			prev = revisions.get(index-1);
		}
		return prev;
	}

        public static String getFileName(String path){
		if(path == null)
			return null;
		String[] tokens = path.split("/");
		if(tokens.length == 0)
			return null;
		String lastPart = tokens[tokens.length - 1];
		return lastPart;
	}

	private static String removeComment(String content){
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(content);
		if(m.find()){
			String comment = m.group();
			String temp = content.replace(comment,"");
			
			int quoMarkCount = 0;
			for (int i = 0; i < temp.length(); i++) {
				if (temp.charAt(i) == '"') quoMarkCount++;
			}
			
			if (quoMarkCount % 2 == 0) {
				content = content.replace(comment,"");
			}
		}
		//remove inline comment in the end of the line
		int offset = content.indexOf("//");
		if (-1 != offset) {
			content = content.substring(0, offset);
		}
		return content;
	}

	public static List<Integer> getAdditionsInHunk(Line line){
		List<Integer> additions = new ArrayList<Integer>();
		String content = prepareLineContent(line);
		for(Line evol : line.getEvolutions()){
			String evolcontent = prepareLineContent(evol);
			if(content.equals(evolcontent)){
				additions.add(evol.getAdditions());
			}
		}
		return additions;
	}
}
