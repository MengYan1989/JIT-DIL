package br.ufrn.raszz.miner.szz;

import static br.ufrn.raszz.util.FileOperationsUtil.getAdditionsInHunk;
import static br.ufrn.raszz.util.FileOperationsUtil.getDiffHunks;
import static br.ufrn.raszz.util.FileOperationsUtil.getHeaders;
import static br.ufrn.raszz.util.FileOperationsUtil.isCommentOrBlankLine;
import static br.ufrn.raszz.util.FileOperationsUtil.isImport;
import static br.ufrn.raszz.util.FileOperationsUtil.joinUnfinishedLines;
import static br.ufrn.raszz.util.FileOperationsUtil.prepareLineContent;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.RepositoryType;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.model.szz.AnnotationGraphModel;
import br.ufrn.raszz.model.szz.BugIntroducingCode;
import br.ufrn.raszz.model.szz.DiffHunk;
import br.ufrn.raszz.model.szz.Line;
import br.ufrn.raszz.model.szz.LineType;
import br.ufrn.raszz.model.szz.SzzFileRevision;
import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.raszz.util.RefacOperationsUtil;
import br.ufrn.razszz.connectoradapter.SzzRepository;

public class AnnotationGraphServiceRaSZZ extends AnnotationGraphService {

	// 增加代码
	private Set<Integer> checkedSZZRevisionIndex = new HashSet<>();
			
	public AnnotationGraphServiceRaSZZ(SzzRepository repository, SzzDAO szzDao, String project, List<String> linkedRevs,
			String repoUrl, String debugPath, String debugContent, SZZImplementationType szzType) {
		super(repository, szzDao, project, linkedRevs, repoUrl, debugPath, debugContent, szzType);
	}

	@Override
	protected void traceBack(AnnotationGraphModel model, 
			LinkedList<SzzFileRevision> fileRevisions) throws Exception {

		final SzzFileRevision fixRev = fileRevisions.getLast();

		System.out.println(fixRev.getRevision());

		// 增加代码
		if (checkerror(fixRev)) return;

		final SzzFileRevision beforeRev = fileRevisions.get(fileRevisions.indexOf(fixRev)-1);
		final ByteArrayOutputStream diff = repository.diffOperation(repoUrl, beforeRev, fixRev);
				
		List<DiffHunk> hunks = null;
		List<String> headers = null;
		try{
			headers = getHeaders(diff);
			hunks = getDiffHunks(diff, 
								 headers, 
								 beforeRev.getPath(), 
								 fixRev.getPath(), 
								 beforeRev.getRevision(), 
								 fixRev.getRevision());	
			joinUnfinishedLines(hunks);

			for(DiffHunk hunk : hunks){
				for(Line linetotrace : hunk.getContent()){
					if(linetotrace.getType() == LineType.DELETION){
						//log.info("line to trace: " + linetotrace.getContent() + "#" + linetotrace.getPreviousNumber() + "- r:" + linetotrace.getRevision() + "- p:" + linetotrace.getPreviousRevision());

						if(!isCommentOrBlankLine(linetotrace.getContent()) && !isImport(linetotrace.getContent())){   
					
							Line startlinetotrace = linetotrace;
							
							//REFACTORING IN BUG-FIX CHANGES
							if (!refacRevProcSet.containsKey(linetotrace.getPreviousRevision())) {
								String commitId = linetotrace.getPreviousRevision();
								List<RefElement> refElements = RefacOperationsUtil.checkUpdateRefactoringDatabase(repository, commitId, project);
								if (refElements != null)
									refacSet.addAll(refElements);
								refacRevProcSet.put(commitId, project);
							}

							boolean isrefac = szzDAO.hasRefacFix(beforeRev.getPath(),
									fixRev.getRevision(), linetotrace.getPreviousNumber(),
									linetotrace.getAdjustmentIndex(), linetotrace.getContent());
							if (isrefac) {
								//log.info(beforeRev.getPath() + " - " + fixRev.getRevision() + " - " + linetotrace.getPreviousNumber() + " - " + linetotrace.getAdjustmentIndex() + " - " + linetotrace.getContent());
								log.info(" Detected REFACTORING in line " + linetotrace.getPreviousNumber() + " of the file " + beforeRev.getPath() + " (" + fixRev.getRevision() + ")");
								//createLinesInPreviousRevisions(model,linetotrace, beforeRev, fileRevisions, fixRev, false, 0, 0, false, null, 0, startlinetotrace);//, false, true);
								break;
							} else {
								try {
									//增加代码
									checkedSZZRevisionIndex = new HashSet<>();
									createLinesInPreviousRevisions(model,
											linetotrace,
											beforeRev, fileRevisions,
											fixRev, false,
											0, 0, false,
											null, 0, startlinetotrace);
								}catch(Exception e){
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			diff.close();
		}
	}

	private SzzFileRevision getPrevRev(SzzFileRevision fileRevision, List<SzzFileRevision> revisions){
		SzzFileRevision prev = null;
		int index = revisions.indexOf(fileRevision);
		//in case the file is not  the first of the collection
		if(index > 0)
			prev = revisions.get(index-1);
		//增加代码
		if (checkedSZZRevisionIndex.contains(index))
			throw new RuntimeException("SZZ have been locked. Give up the line!");
		checkedSZZRevisionIndex.add(index);
		return prev;
	}
	
	private void createLinesInPreviousRevisions(AnnotationGraphModel model, Line linetotrace,
												SzzFileRevision rev,
												LinkedList<SzzFileRevision> fileRevisions,
												SzzFileRevision fixRev, boolean isrefac,
												int indexPosRefac, int indexChangePath,
												boolean isReTrace, SzzFileRevision reprevrev,
												int indexFurtherBack, Line startline) throws Exception{

		Line prevline = null;
		String content = prepareLineContent(linetotrace);
		SzzFileRevision prevrev = (!isReTrace)? getPrevRev(rev,fileRevisions) : reprevrev;
		//if the buggycode is in rev from the start
		//we have to persist it when prevrev == null

		// 增加代码
		if (checkBadCommit(prevrev))
			throw new RuntimeException("cannot do this commit");


		if(prevrev != null) {
			LinkedList<Line> previousLines = model.get(prevrev);
			//#DEBUG
			if (debugContent != null && linetotrace.getContent().equals(debugContent)) 
				log.info("DEBUG POINT IN: " + prevrev.getRevision());
						
			//REFACTORING IN FIX-INDUNCING CHANGES
			if (indexPosRefac >= 0 && !refacRevProcSet.containsKey(linetotrace.getPreviousRevision())) {
				String commitId = linetotrace.getPreviousRevision();
				List<RefElement> refElements = RefacOperationsUtil.checkUpdateRefactoringDatabase(repository, commitId, project);
				if (refElements != null)
					refacSet.addAll(refElements);
				refacRevProcSet.put(commitId, project);
			}

			List<RefElement> currentRefacSet = RefacOperationsUtil.filterRefacSet(refacSet, linetotrace.getPreviousPath(), linetotrace.getPreviousRevision(), linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content); 
			if (currentRefacSet != null && currentRefacSet.size() != 0) {
				String prevrefpath = RefacOperationsUtil.prevRefacPath(refacSet, linetotrace.getPreviousPath(), linetotrace.getPreviousRevision(), linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content);
				if (prevrefpath != null && !prevrev.getPath().equals(prevrefpath)) {
					retrace(model, linetotrace, fileRevisions, fixRev, isrefac, indexPosRefac, indexChangePath, rev, prevrefpath, indexFurtherBack, startline);
					return;
				}
				String prevrefcontent = RefacOperationsUtil.prevRefacContent(currentRefacSet, content);
				content = prevrefcontent;
				isrefac = true;
			} else isrefac = false;			
			indexPosRefac += (isrefac || indexPosRefac >0)?1:0;
			
			boolean matchFound = false;
			for(Line line : previousLines){
				String prevcontent = prepareLineContent(line);
				if(content.equals(prevcontent)){
					if (isrefac) {	
						long[] interval = RefacOperationsUtil.prevRefacLines(currentRefacSet);
						if(interval[0] <= line.getPreviousNumber() && line.getPreviousNumber() <= interval[1]){
							prevline = line; 
							if(prevline != null){
								matchFound = true;
								//found match by refactoring
								createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
										fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline);
								break;
							}
						}
					} else {			
						if(line.getNumber() != -1){
							//we have to find where the exact code was introduced this is why we don't care about evolutions array						
							//because it means that the code have changed from the previous revision
							if(line.getNumber() == linetotrace.getPreviousNumber()){
								prevline = line; 
								if(prevline != null){
									matchFound = true;
									//log.info(" found a match to [" + line.getNumber() + ", rev: " + prevrev.getRevision()
										//	+ "] prev_line_content = " + prevcontent);
									//recursive call to traceback
									createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
											fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline);
									break;
								}
							} 
						} else if( linetotrace.getPreviousNumber() == (line.getPreviousNumber()
									+line.getContext_difference() + line.getPosition())) {
							prevline = line;
							if(prevline != null){
								matchFound = true;
								//c.readLine("found match by content and context adjustment!");
								//log.info("found match by content and context adjustment in rev: " + prevrev.getRevision());
								//recursive call to traceback
								createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
										fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline);
								break;
							}
						} else { //last match attempt
							//#debug
							//c.readLine("last match attempt... trying to do evolution tracing");
							List<Integer> additions = getAdditionsInHunk(line);
	
							if(!additions.isEmpty()){
								for(Integer addition : additions){
									int position = addition - line.getDeletions();
									if(linetotrace.getPreviousNumber() == (line.getPreviousNumber() + 
												line.getContext_difference() + position)){
										prevline = line;
										if(prevline != null){
											matchFound = true;
											//log.info("found match by evolution trace!");
											createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
													fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline);
											break;
										}
									}
								}
							} else {
								//#debug
								//c.readLine("trying local evolution trace!");							
								int position = 0;
								if(line.getContext_difference() > 0)
									position = line.getDeletions() - line.getAdditions();
								else
									position = line.getAdditions() - line.getDeletions();
	
								if(linetotrace.getPreviousNumber() == (line.getPreviousNumber() + line.getContext_difference() + position)){
									prevline = line;
									if(prevline != null){
										matchFound = true;
										//log.info("found match by local evolution trace!");
										createLinesInPreviousRevisions(model, prevline, prevrev, fileRevisions, 
												fixRev, isrefac, indexPosRefac, indexChangePath, false, null, ++indexFurtherBack, startline);
										break;
									}
							    }
							}	
						}
					}
				} else {					
					//se nao for igual mas for refac?
					//mas precisa ser um refac do conserto
					//e como descobrir a versao anterior do codigo antes do refac?
					
				}
			} 
			if(!matchFound){
				createBicode(rev, fixRev, linetotrace, indexPosRefac, indexChangePath, isrefac, indexFurtherBack, startline);
			}
		} else {
			String prevrefpath = RefacOperationsUtil.prevRefacPath(refacSet, linetotrace.getPreviousPath(), linetotrace.getPreviousRevision(), linetotrace.getPreviousNumber(), linetotrace.getAdjustmentIndex(), content);
			if (prevrefpath != null) {
				retrace(model, linetotrace, fileRevisions, fixRev, isrefac, indexPosRefac, indexChangePath, rev, prevrefpath, indexFurtherBack, startline);
			} else {
				createBicode(rev, fixRev, linetotrace, indexPosRefac, indexChangePath, isrefac, indexFurtherBack, startline);
			}
		}
	}
	
	private void retrace(AnnotationGraphModel model, Line linetotrace, LinkedList<SzzFileRevision> fileRevisions, SzzFileRevision fixRev, boolean isrefac, int indexPosRefac,
			int indexChangePath, SzzFileRevision rev, String prevrefpath, int indexFurtherBack, Line startline) throws Exception{
		//fileRevisions = extractSZZFilesFromPath(prevrefpath, linetotrace.getPreviousRevision()-1, true);
		if (repository.getConnectorType() == RepositoryType.SVN)
			fileRevisions = repository.extractSZZFilesFromPath(repoUrl, prevrefpath, (Long.parseLong(linetotrace.getPreviousRevision())-1)+"", true);
		else fileRevisions = null;
		if (fileRevisions != null) {
			AnnotationGraphBuilder agb = new AnnotationGraphBuilder();
			model = agb.buildLinesModel(repository, fileRevisions, repoUrl, project);
			SzzFileRevision reprevrev = fileRevisions.getLast();
			createLinesInPreviousRevisions(model, linetotrace, rev, fileRevisions, 
					fixRev, isrefac, indexPosRefac, ++indexChangePath, true, reprevrev, ++indexFurtherBack, startline);
		}
	}
	
	private BugIntroducingCode createBicode(SzzFileRevision rev, SzzFileRevision fixRev, Line line, 
			int indexPosRefac, int indexChangePath, boolean isrefac, int indexFurtherBack, Line startline){

		// 增加代码
		if (checkBadCommit(rev))
			return null;

		BugIntroducingCode b = new BugIntroducingCode();
		b.setFixRevision(fixRev.getRevision());
		b.setContent(line.getContent());

		b.setLinenumber(line.getPreviousNumber());
		b.setPath(rev.getPath());
		b.setRevision(rev.getRevision());

		b.setProject(this.project);
		b.setAdjustmentIndex(line.getAdjustmentIndex());
		b.setIndexPosRefac(indexPosRefac);
		b.setIndexChangePath(indexChangePath);
		//String toparse = rev.getRevisionProperties().getStringValue(SVNRevisionProperty.DATE);
		//Date creation = FileOperationsUtil.getRevisionDate(toparse);
		//b.setSzzDate(creation);
		b.setSzzDate(rev.getCreateDate());
		b.setIsrefac(isrefac);
		b.setIndexFurtherBack(indexFurtherBack);

		b.setStartRevision(startline.getPreviousRevision());
		b.setStartContent(startline.getContent());
		b.setStartPath(startline.getPreviousPath());
		b.setStartlinenumber(startline.getPreviousNumber());

		bicodes.add(b);
		return b;
	}

	// 增加代码
	private boolean checkerror(SzzFileRevision fixRev){
		if (fixRev.getRevision().equals("814cb1f024130c048331cec876cfc8d7ddaab99e"))
			return true;
		else
			return false;
	}

	// 增加代码
	private boolean checkBadCommit(SzzFileRevision introRev){
		if (introRev.getRevision().equals("0e60b7d9888217be8499fe8b13c7b55e707f269c"))
			return true;
		return false;
	}
}
