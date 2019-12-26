package cs.zju.analysis;


import core.connector.service.GitService;
import core.connector.service.impl.GitServiceImpl;
import cs.zju.config.Config;
import cs.zju.utils.MyGitUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.util.*;

public class PmdMain {
    private static final List<String> projects = Config.getProjects();
    private static List<String> repoUrls = Config.getCloneUrls();

    public static void main(String[] args) throws Exception{

        if (projects.size() != repoUrls.size())
            throw new RuntimeException("Project size and Url size are not equal");
        for (int i = 0; i < projects.size(); i++){
            String project = projects.get(i);
            String repoUrl = repoUrls.get(i);
            String projectPath = Config.projectPath(project);
            List<String> allCommits = MyGitUtils.getAllCommits(projectPath, repoUrl, true);

            for (String c: allCommits){
                PmdAnalysis pa = new PmdAnalysis(project, c, projectPath, repoUrl);
                pa.analysis();
            }
        }
    }



}