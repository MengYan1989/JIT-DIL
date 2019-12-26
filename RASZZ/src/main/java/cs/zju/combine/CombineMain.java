package cs.zju.combine;

import br.ufrn.raszz.model.szz.*;
import br.ufrn.raszz.model.szz.PmdRevisionResult.PmdFileResultMap;
import br.ufrn.raszz.persistence.HibernateUtil;
import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.raszz.persistence.SzzDAOImpl;
import br.ufrn.raszz.util.CsvOperationsUtil;
import br.ufrn.raszz.util.FileOperationsUtil;
import br.ufrn.razszz.connectoradapter.GitRepositoryAdapter;
import br.ufrn.razszz.connectoradapter.SzzRepository;
import core.connector.service.GitService;
import core.connector.service.impl.GitServiceImpl;
import core.connector.util.GitOperationsUtil;
import cs.zju.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static br.ufrn.raszz.util.FileOperationsUtil.isTestFile;
import static br.ufrn.raszz.util.FileOperationsUtil.runRegex;

public class CombineMain {
    private static final List<String> projects = Config.getProjects();
    private static List<String> repoUrls = Config.getCloneUrls();

    private static final String[] headers = {"commit_hash", "content", "file_pre", "file_new",
    "line_num", "author", "time", "bug_introducing", "commit_label", "pmd"};

    public static void main(String[] args) throws Exception{
        if (projects.size() != repoUrls.size())
            throw new RuntimeException("Project size and Url size are not equal");
        for (int i = 0; i < projects.size(); i++){
            String project = projects.get(i);
            String repoUrl = repoUrls.get(i);
            String projectPath = Config.projectPath(project);

            List<String[]> csvData = new ArrayList<>();

            // 得到全部的bug introducing
            SzzDAO szzDAO = new SzzDAOImpl();
            Map<String, List<SimpleBugIntroducingChange>> bugIntroducingDataMap = szzDAO.
                    getBugIntroducingChangeFromRASZZ(project);

            GitService gitService = new GitServiceImpl();
            Repository repository = gitService.cloneIfNotExists(projectPath, repoUrl);

            SzzRepository szzRepository = new GitRepositoryAdapter(repository, projectPath, repoUrl);

            RevWalk walk = gitService.createAllRevsWalk(repository);
            Iterator<RevCommit> commitIter = walk.iterator();
            Set<String> analyzedCommits = new HashSet<>();
            while(commitIter.hasNext()){
                RevCommit revCommit = commitIter.next();

                // Merge Commit 和 首个Commit被去除
                if (revCommit.getParentCount() > 1 || revCommit.getParentCount() == 0)
                    continue;
                String commitId = revCommit.getId().getName();
                if (analyzedCommits.contains(commitId))
                    continue;

                System.out.println(commitId);

                boolean commitIsBugIntroducing = bugIntroducingDataMap.containsKey(commitId);
                Map<String, Set<Long>> fileBugIntroducingLineMap = new HashMap<>();
                if (commitIsBugIntroducing) {
                    // 得到所有的当前commit引入缺陷的line
                    List<SimpleBugIntroducingChange> commitBugIntroducingLines = bugIntroducingDataMap.get(commitId);

                    for (SimpleBugIntroducingChange sbic : commitBugIntroducingLines) {
                        String filePath = sbic.getPath();
                        long lineNumber = sbic.getLineNumber();
                        if (!fileBugIntroducingLineMap.containsKey(filePath))
                            fileBugIntroducingLineMap.put(filePath, new HashSet<>());
                        fileBugIntroducingLineMap.get(filePath).add(lineNumber);
                    }
                }

                analyzedCommits.add(commitId);
                RevCommit baseCommit = revCommit.getParent(0);
                String baseCommitId = baseCommit.getId().getName();
                Map<String, String> curBeforeFileMap = gitService.getCurBeforeFileMap(repository,
                        commitId);
                List<PmdLineResult> curCommitPmdLineResult = PmdLineResult.getPmdLineResultFromCSV(project,
                        commitId);

                if (curCommitPmdLineResult == null)
                    curCommitPmdLineResult = new ArrayList<>();

                PmdRevisionResult prr = new PmdRevisionResult();
                prr.setRevision(commitId);
                prr.setLineResults(curCommitPmdLineResult);
                PmdFileResultMap map = new PmdFileResultMap();
                map.fromPmdRevisionResult(prr);

                for (String newPath: curBeforeFileMap.keySet()){
                    if (isTestPath(newPath, repoUrl, commitId, szzRepository))
                        continue;

                    System.out.println(newPath);

                    ByteArrayOutputStream diffContent = GitOperationsUtil.diffOperation2(repository,
                            newPath, commitId, baseCommitId);
                    String oldPath = curBeforeFileMap.get(newPath);
                    List<String> headers = FileOperationsUtil.getHeaders(diffContent);
                    List<DiffHunk> hunks = FileOperationsUtil.getDiffHunks(diffContent, headers,
                            oldPath, newPath, baseCommitId, commitId);
                    assignPmdLevelToHunks(hunks, map, newPath);
                    joinUnfinishedLines(hunks);

                    Set<Long> bugIntroducingLineSet = fileBugIntroducingLineMap.get(newPath);
                    csvData.addAll(getCombineRecordsFromHunks(revCommit, oldPath, newPath,
                            commitIsBugIntroducing, bugIntroducingLineSet, hunks));
                }
            }

            String[] csvHeaders = CombineRecord.getHeaders();
            String projectResultPath = Config.resultPath(project);
            CsvOperationsUtil.writeCSV(projectResultPath, csvHeaders, csvData);
        }
        HibernateUtil.shutdown();
    }

    public static void assignPmdLevelToHunks(List<DiffHunk> hunks, PmdFileResultMap map, String newPath){
        for (DiffHunk hunk: hunks){
            List<Line> lines = hunk.getContent();
            for(Line l: lines){
                if (l.getType().equals(LineType.ADDITION)){
                    long lineNumber = l.getNumber();
                    int pmdLineLevel = map.getLineLevel(newPath, lineNumber);
                    l.setPmdLevel(pmdLineLevel);
                }
            }
        }
    }

    public static void joinUnfinishedLines(List<DiffHunk> hunks){
        for(DiffHunk hunk : hunks){
            List<Line> lines = hunk.getContent();
            FileOperationsUtil.joinUnfinishedLinesWhenCloning(lines, true, true);
        }
    }

    public static List<String[]> getCombineRecordsFromHunks(RevCommit commit,
                                                            String oldPath,
                                                            String newPath,
                                                            boolean isBugIntroducingCommit,
                                                            Set<Long> bugIntroducingLineSet,
                                                            List<DiffHunk> hunks){
        List<String[]> ret = new ArrayList<>();
        for (DiffHunk hunk: hunks){
            List<Line> lines = hunk.getContent();
            for(Line l: lines){
                if (l.getType().equals(LineType.ADDITION)){
                    if (l.getContent().trim().equals(""))
                        continue;
                    // Remove Comment or Blank Lines
                    if(FileOperationsUtil.isCommentOrBlankLine(l.getContent()))
                        continue;

                    // Remove Import Lines
                    if(FileOperationsUtil.isImport(l.getContent()))
                        continue;

                    String content = prepareContent(l.getContent());
                    content = content.trim();

                    if (content.length() < 10)
                        continue;

                    long lineNumber = l.getNumber();
                    CombineRecord record = new CombineRecord();
                    record.setCommitHash(commit.getId().getName());
                    record.setContent(content);
                    record.setPreFilePath(oldPath);
                    record.setNewFilePath(newPath);
                    record.setLineNumber(lineNumber);
                    record.setAuthor(commit.getAuthorIdent().getName());
                    record.setTime(new Date(commit.getCommitTime()));
                    record.setBugIntroducing(false);
                    if (bugIntroducingLineSet != null && bugIntroducingLineSet.contains(lineNumber))
                        record.setBugIntroducing(true);
                    record.setCommitLabel(isBugIntroducingCommit);
                    record.setPmdLevel(l.getPmdLevel());
                    ret.add(record.toCsvRecord());
                }
            }
        }
        return ret;
    }

    public static boolean isTestPath(String path,
                                     String repoUrl,
                                     String commitId,
                                     SzzRepository szzRepository) throws Exception{
        String fname = FileOperationsUtil.getFileName(path);
        if(fname == null || !fname.contains(".java") || fname.trim().endsWith(".javajet"))
            return true;
        // 新加条件
        // 路径中含有test即排除
        String[] subPaths = StringUtils.split(path, '/');
        for (String sp: subPaths) {
            String tempSP = sp.toLowerCase();
            if (tempSP.equals("test") || tempSP.equals("tests"))
                return true;
        }

        ByteArrayOutputStream baous = szzRepository.catOperation(repoUrl, path, commitId);

        //we are also not interested on testFiles
        if(runRegex(path,"Test.java$") || FileOperationsUtil.isTestFile(baous)){
            return true;
        }

        baous.close();

        return false;
    }

    public static String prepareContent(String line){
        String content = line.trim();
        if(content.length() > 0){
            if(content.charAt(0) == '+'){
                content = content.replaceFirst("\\+","");
            } else if (content.charAt(0) == '-'){
                content = content.replaceFirst("-","");
            }
        }
        content = content.replaceAll("\\s++", " ");
        content = content.trim();
        return content;
    }
}
