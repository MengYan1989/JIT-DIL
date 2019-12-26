package br.ufrn.raszz.miner.refdiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.ufrn.raszz.model.RefCaller;
import br.ufrn.raszz.model.RefElement;
import org.eclipse.jgit.lib.Repository;
import org.hibernate.Transaction;
import org.tmatesoft.svn.core.io.SVNRepository;

import br.ufrn.raszz.persistence.DAOType;
import br.ufrn.raszz.persistence.FactoryDAO;
import br.ufrn.raszz.persistence.RefacDAO;
import br.ufrn.razszz.connectoradapter.SzzRepository;
import core.connector.service.GitService;
import core.connector.service.SvnService;
import core.connector.service.impl.GitServiceImpl;
import core.connector.service.impl.SvnServiceImpl;
import refdiff.core.RefDiff;
import refdiff.core.RefDiffSVN;
import refdiff.core.rm2.model.SDEntity;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

public class RefDiffService {

	private RefacDAO refacDAO;
	private SzzRepository repository;

	public RefDiffService(SzzRepository repository) {
		refacDAO = (FactoryDAO.getFactoryDAO(DAOType.HIBERNATE)).getRefacDAO();
		this.repository = repository; 
	}
	
	public List<RefElement> executeRefDiff(String project, String commitId, String revisionType) {
		synchronized (refacDAO) {
			List<RefElement> refElements = null;
			switch (repository.getConnectorType()) {
				case GIT:
					refElements = executeGit(repository.getUrl(), repository.getRepositoryFolder(), commitId, project, revisionType);
					Transaction tx1 = refacDAO.beginTransaction();
					refacDAO.insertRefDiffRevisionsProcessed(project, commitId);
					tx1.commit();
					break;
				case SVN:
					refElements = executeSvn(repository.getUrl(), repository.getUser(), repository.getPassword(), project, commitId, repository.getRepositoryFolder(), revisionType);
					Transaction tx2 = refacDAO.beginTransaction();
					refacDAO.insertRefDiffRevisionsProcessed(project, commitId);
					tx2.commit();
					break;
			}
			return refElements;
		}
	}

	private List<RefElement> executeGit(String repoUrl, String repoFolder, String commitId, String project, String revisionType) { 
		RefDiff refDiff = new RefDiff();
		GitService gitService = new GitServiceImpl();
		try (Repository repository = gitService.cloneIfNotExists(repoFolder, repoUrl)) { //"C:/tmp/clojure2", "C:/tmp/clojure2.git"
			List<SDRefactoring> refactorings = refDiff.detectAtCommit(repository, commitId); //"17217a1"
			System.out.println(refactorings);
			return saveResults(refactorings, project, repoFolder, commitId, revisionType);
			/*for (SDRefactoring refactoring : refactorings) {
				System.out.println(refactoring.toString() + " " + refactoring.getEntityAfter().sourceCode().toString());
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private List<RefElement> executeSvn(String repoUrl, String user, String password, String project, String commitId,
			String folder, String revisionType) {
		RefDiffSVN refDiffSVN = new RefDiffSVN();
		SvnService svnService = new SvnServiceImpl();
		try {
			SVNRepository repository = svnService.openRepository(repoUrl, user, password);
			List<SDRefactoring> refactorings = refDiffSVN.detectAtCommit(repository, commitId.toString(), folder);
			return saveResults(refactorings, project, folder, commitId, revisionType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<RefElement> saveResults(List<SDRefactoring> refactorings, String project, String folder, String commitId, String revisionType)
			throws IOException {
		synchronized (refacDAO) {
			List<RefElement> refElements = new ArrayList<RefElement>();
			// List<RefCaller> callers = new ArrayList<>();
			Transaction tx = refacDAO.beginTransaction();
			for (SDRefactoring refactoring : refactorings) {
				RefactoringDataBuilder refacDataBuilder = new RefactoringDataBuilder();
				RefElement element = refacDataBuilder.prepareElement(refactoring, project, commitId);
				refacDAO.saveRefDiffResults(element, revisionType);
				saveCallers(refactoring.getEntityAfter(), element.getCallerList(), revisionType);
				refElements.add(element);
				//printdebug(folder, element);

			}
			tx.commit();
			if (refactorings != null)
				// svnDAO.saveRefDiffResults(refElements);
				// svnDAO.saveCallersRefDiffResults(callers);
				System.err.println("============ (" + refactorings.size()
						+ " refactoring data saved with success for commitId: " + commitId + ") ===========");

			return refElements;
		}
	}

	private void saveCallers(SDEntity entity, List<RefCaller> callers, String revisionType) throws IOException {
		for (RefCaller caller : callers) {
			refacDAO.saveCallersRefDiffResults(caller, revisionType);
		}
	}

	
}
