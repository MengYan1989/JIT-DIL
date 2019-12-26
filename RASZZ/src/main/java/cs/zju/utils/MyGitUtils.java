package cs.zju.utils;

import core.connector.service.GitService;
import core.connector.service.impl.GitServiceImpl;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.util.*;

public class MyGitUtils {
    public static List<String> getAllCommits(String projectPath, String repoUrl, boolean noMerge) throws Exception{
        GitService gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(projectPath, repoUrl);
        RevWalk walk = gitService.createAllRevsWalk(repository);
        Iterator<RevCommit> iter = walk.iterator();
        List<String> results = new ArrayList<>();
        Set<String> addCommits = new HashSet<>();
        while(iter.hasNext()){
            RevCommit commit = iter.next();
            String commitId = commit.getId().getName();
            if (addCommits.contains(commitId))
                continue;
            if (noMerge && commit.getParentCount() > 1)
                continue;
            results.add(commitId);
            addCommits.add(commitId);
        }
        return results;
    }
}
