package refdiff.core.rm2.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.connector.service.GitService;
import core.connector.service.impl.GitServiceImpl;
import refdiff.core.rm2.model.SDModel;

public class GitHistoryStructuralDiffAnalyzer {

	Logger logger = LoggerFactory.getLogger(GitHistoryStructuralDiffAnalyzer.class);
	private final RefDiffConfig config;
	
	public GitHistoryStructuralDiffAnalyzer() {
        this(new RefDiffConfigImpl());
    }
	
	public GitHistoryStructuralDiffAnalyzer(RefDiffConfig config) {
        this.config = config;
    }

    private void detect(GitService gitService, Repository repository, final StructuralDiffHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();
		
		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				detectRefactorings(gitService, repository, handler, projectFolder, currentCommit);
				
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				handler.handleException(currentCommit.getId().getName(), e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d]", projectName, commitsCount, errorCommitsCount));
			}
		}

		handler.onFinish(commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d]", projectName, commitsCount, errorCommitsCount));
	}

	public void detectAll(Repository repository, String branch, final StructuralDiffHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	public void fetchAndDetectNew(Repository repository, final StructuralDiffHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.fetchAndCreateNewRevsWalk(repository);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	public void detectAtCommit(Repository repository, String commitId, StructuralDiffHandler handler) {
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		//RevWalk walk = new RevWalk(repository);
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));			
			walk.parseCommit(commit.getParent(0));
			this.detectRefactorings(gitService, repository, handler, projectFolder, commit);
		} catch (Exception e) {
		    logger.warn(String.format("Ignored revision %s due to error", commitId), e);
		    handler.handleException(commitId, e);
        }
	}
	
	protected void detectRefactorings(GitService gitService, Repository repository, final StructuralDiffHandler handler, File projectFolder, RevCommit currentCommit) throws Exception {
	    String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent, renamedFilesHint, false);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring
		
		SDModelBuilder builder = new SDModelBuilder(config);
		if (filesBefore.isEmpty() || filesCurrent.isEmpty()) {
		    return;
		}
			// Checkout and build model for current commit
	    File folderAfter = new File(projectFolder.getParentFile(), "v1/" + projectFolder.getName() + "-" + commitId.substring(0, 7));
		gitService.checkoutToSeparatePath(folderAfter, projectFolder, commitId);
		if (folderAfter.exists()) {
	        System.out.println(String.format("Analyzing code after (%s) ...", commitId) + " " + new Date());
	        builder.analyzeAfter(folderAfter, filesCurrent);
	        FileUtils.deleteDirectory(folderAfter);

	    } else {
	        gitService.checkout(repository, commitId);
	        System.out.println(String.format("Analyzing code after (%s) ...", commitId) + " " + new Date());
	        builder.analyzeAfter(projectFolder, filesCurrent);
	    }
	
	    String parentCommit = currentCommit.getParent(0).getName();
		File folderBefore = new File(projectFolder.getParentFile(), "v0/" + projectFolder.getName() + "-" + commitId.substring(0, 7));
		gitService.checkoutToSeparatePath(folderBefore, projectFolder, parentCommit);
		if (folderBefore.exists()) {
		    System.out.println(String.format("Analyzing code before (%s) ...", parentCommit)+ " " + new Date());
            builder.analyzeBefore(folderBefore, filesBefore);
            FileUtils.deleteDirectory(folderBefore);
		} else {
		    // Checkout and build model for parent commit
		    gitService.checkout(repository, parentCommit);
		    logger.info(String.format("Analyzing code before (%s) ...", parentCommit)+ " " + new Date());
		    builder.analyzeBefore(projectFolder, filesBefore);
		}

		final SDModel model = builder.buildModel();
		handler.gitHandle(currentCommit, model);
	}

}
