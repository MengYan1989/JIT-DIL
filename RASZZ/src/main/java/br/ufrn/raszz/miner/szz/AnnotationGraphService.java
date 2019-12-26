package br.ufrn.raszz.miner.szz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import br.ufrn.raszz.model.RefElement;
import br.ufrn.raszz.model.SZZImplementationType;
import br.ufrn.raszz.model.szz.AnnotationGraphModel;
import br.ufrn.raszz.model.szz.BugIntroducingCode;
import br.ufrn.raszz.model.szz.SzzFileRevision;
import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.razszz.connectoradapter.SzzRepository;

public abstract class AnnotationGraphService implements Runnable {

	protected static final Logger log = Logger.getLogger(AnnotationGraphService.class);
	protected static SzzDAO szzDAO;
	protected SzzRepository repository;
	protected String project;
	protected String repoUrl;
	protected List<BugIntroducingCode> bicodes;
	protected List<RefElement> refacSet;
	protected Map<String, String> refacRevProcSet;
	protected List<String> linkedRevs;
	protected String debugPath;
	protected String debugContent;
	protected SZZImplementationType szzType;

	public AnnotationGraphService(SzzRepository repository, SzzDAO szzDao,
			String project, List<String> linkedRevs, String repoUrl, 
			String debugPath, String debugContent, SZZImplementationType szzType) {
		this.repository = repository;
		this.setDao(szzDao);
		this.project = project;
		this.repoUrl = repoUrl;
		this.linkedRevs = linkedRevs;
		this.debugPath = debugPath;
		this.debugContent = debugContent;
		this.szzType = szzType;
	}

	private static void setDao(SzzDAO dao){
		szzDAO = dao;
	}

	@Override
	public void run() {
		try {
			log.info(Thread.currentThread().getName() + "-" + project + " is running.");
			long startTime = System.nanoTime();
			buildAnnotationGraph(linkedRevs);
			long endTime = System.nanoTime();
			log.info("duration: " + (endTime - startTime));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(" Cannot solve");
		}
	}

	private boolean buildAnnotationGraph(List<String> linkedRevs) throws Exception {
		//did the process start before?
		List<String> processedRevisions = szzDAO.getAllRevisionProcessed(project);
	
		refacSet = (linkedRevs.size() != 0)? szzDAO.getRefacBic(project): new ArrayList<RefElement>();
		refacRevProcSet = (linkedRevs.size() != 0)? szzDAO.getAllRefDiffRevisionsProcessed(project): new HashMap<String,String>();
		
		System.out.println("Project " + project + " starting... with " + refacSet.size() + " refactorings...");
		System.out.println(linkedRevs.size() + " Linked revisions found...");
		long count = 1;
		for (String i : linkedRevs) {

			//in case we needed to stop the process
			if (processedRevisions.contains(i)) {
				log.info("Revision " + i + " was processed already!");
				count++;
				continue;
			}

			AnnotationGraphModel model = new AnnotationGraphModel();			
			
			bicodes = new ArrayList<BugIntroducingCode>();
			List<String> paths = repository.getChangedPaths(i);
			for(String path: paths) {

				System.out.println("path: " + path + "(#" + i + ")");
				if (debugPath != null && !path.equals(debugPath)) continue;
				try {
					final LinkedList<SzzFileRevision> szzFileRevisions = repository.extractSZZFilesFromPath(repoUrl, path, i, false);
					System.out.println(szzFileRevisions);
					if (szzFileRevisions == null) continue;
					AnnotationGraphBuilder agb = new AnnotationGraphBuilder();
					model = agb.buildLinesModel(repository, szzFileRevisions, repoUrl, project);
					traceBack(model,szzFileRevisions);
					log.info( "bics: " + bicodes.size()); 
				} catch (Exception e) {
					log.error("Error in the path: " + path);
					continue;
				}
			}
			synchronized(szzDAO){
				Transaction tx = szzDAO.beginTransaction();
				for(BugIntroducingCode bicode : bicodes){
					szzDAO.insertBugIntroducingCode(bicode, szzType);
				}
				szzDAO.insertProjectRevisionsProcessed(project, i);
				tx.commit();
			}
			log.info(count++ + " processed revisions of " + linkedRevs.size() + " for project " + project);
			
		}
		return true;
	}		
	
	protected abstract void traceBack(AnnotationGraphModel model, 
			LinkedList<SzzFileRevision> fileRevisions) throws Exception;
	
}
