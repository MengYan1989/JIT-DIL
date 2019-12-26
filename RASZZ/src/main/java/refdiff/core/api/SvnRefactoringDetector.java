package refdiff.core.api;

import java.util.List;

import org.tmatesoft.svn.core.io.SVNRepository;

import refdiff.core.rm2.model.refactoring.SDRefactoring;

/**
 * Detect refactorings in a git commit.
 * 
 */
public interface SvnRefactoringDetector {

	/**
	 * Detect refactorings performed in the specified commit. 
	 * 
	 * @param repository A git repository (from JGit library).
	 * @param commitId The SHA key that identifies the commit.
	 * @param handler A handler object that is responsible to process the detected refactorings. 
	 */
	//void detectAtCommit(SVNRepository repository, String commitId, RefactoringHandler handler);
	List<SDRefactoring> detectAtCommit(SVNRepository repository, String commitId, String folder);

	/**
	 * @return An ID that represents the current configuration for the algorithm in use.
	 */
	String getConfigId();
}
