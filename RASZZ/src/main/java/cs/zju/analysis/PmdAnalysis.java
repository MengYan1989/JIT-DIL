package cs.zju.analysis;


import br.ufrn.raszz.model.szz.PmdLineResult;
import br.ufrn.raszz.util.CsvOperationsUtil;
import br.ufrn.raszz.util.FileOperationsUtil;
import br.ufrn.razszz.connectoradapter.GitRepositoryAdapter;
import br.ufrn.razszz.connectoradapter.SzzRepository;
import core.connector.service.GitService;
import core.connector.service.impl.GitServiceImpl;
import core.connector.util.ExternalProcess;
import cs.zju.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Repository;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static br.ufrn.raszz.util.FileOperationsUtil.isTestFile;
import static br.ufrn.raszz.util.FileOperationsUtil.runRegex;

public class PmdAnalysis {
    private String project;
    private String revision;

    private GitService gitService;

    private String repoUrl;

    private File projectFolder;
    private File checkoutRevFolder;
    private File analyzeFileListDir;

    private String analyzeFileListPath;
    private String tmpResultStoragePath;
    private String resultStoragePath;

    private SzzRepository szzRepository;
    private List<String> pathsToAnalyze;


    public PmdAnalysis(String project,
                       String revision,
                       String projectPath,
                       String repoUrl) throws Exception{
        this.project = project;
        this.revision = revision;
        gitService = new GitServiceImpl();
        Repository repository = gitService.cloneIfNotExists(projectPath, repoUrl);
        this.projectFolder = new File(projectPath);
        assignCheckoutFile();
        szzRepository = new GitRepositoryAdapter(repository, projectPath, repoUrl);
        pathsToAnalyze = szzRepository.getChangedPaths(revision);
        this.repoUrl = repoUrl;
    }

    private void assignCheckoutFile(){
        String basePath = this.projectFolder.getParentFile().getAbsolutePath();
        File baseFile = new File(basePath);
        checkoutRevFolder = new File(baseFile, "checkout/tmp-" + revision);
    }

    private void assignPmdRelatedPaths(){
        String basePath = this.projectFolder.getParentFile().getAbsolutePath();
        File baseFile = new File(basePath);
        analyzeFileListDir = new File(baseFile, "pmd/tmp-" + revision);
        if (!analyzeFileListDir.exists())
            analyzeFileListDir.mkdirs();
        analyzeFileListPath = new File(analyzeFileListDir, "fileList.txt").getAbsolutePath();
        tmpResultStoragePath = new File(baseFile, "pmd/tmp-result.csv").getAbsolutePath();
        resultStoragePath = Config.projectPmdResultPath(project, revision);
    }

    private void checkout() throws Exception{
//        gitService.checkoutToSeparatePath(checkoutRevFolder,
//                projectFolder, revision);
        if (!checkoutRevFolder.exists())
            checkoutRevFolder.mkdirs();
        for (String path: pathsToAnalyze){
            String newPath = new File(checkoutRevFolder,
                    "" +pathsToAnalyze.indexOf(path)+ ".java").getAbsolutePath();
            ByteArrayOutputStream baous = szzRepository.catOperation(repoUrl, path, revision);
            OutputStream out = new FileOutputStream(newPath);
            baous.writeTo(out);
            out.close();
            baous.close();
        }
    }

    public void analysis() throws Exception{
        filterPaths();
        if (pathsToAnalyze.size() > 0) {
            assignPmdRelatedPaths();

            // First checkout
            checkout();

            // Write paths to analysis into tmp file
            writePathToTmpFile();

            // External call pmd
            callPmdCommand();

            writeFinalResultToCSV();

            // Remove checkout folder
            FileUtils.deleteDirectory(checkoutRevFolder);
            FileUtils.deleteDirectory(analyzeFileListDir);
            new File(tmpResultStoragePath).delete();
        }
    }

    private void filterPaths() throws Exception{
        List<String> nonTestPaths = new ArrayList<>();
        for (String p: pathsToAnalyze){
            if (!isTestPath(p, repoUrl))
                nonTestPaths.add(p);
        }
        pathsToAnalyze = new ArrayList<>(nonTestPaths);
    }

    private boolean isTestPath(String path, String repoUrl) throws Exception{
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

        ByteArrayOutputStream baous = szzRepository.catOperation(repoUrl, path, revision);

        //we are also not interested on testFiles
        if(runRegex(path,"Test.java$") || isTestFile(baous)){
            return true;
        }

        baous.close();

        return false;
    }

    private void writePathToTmpFile() throws Exception{
        FileWriter writer = new FileWriter(analyzeFileListPath);
        System.out.println(pathsToAnalyze);
        for (String path: pathsToAnalyze){
            String fullPath = new File(checkoutRevFolder,
                    ""+pathsToAnalyze.indexOf(path)+".java").getAbsolutePath();
            if (!new File(fullPath).exists())
                throw new RuntimeException("cannot find the file!");
            writer.write(fullPath);
            if (pathsToAnalyze.indexOf(path) != pathsToAnalyze.size()-1)
                writer.write(",");
        }
        writer.close();
    }

    private void callPmdCommand(){
        System.out.println(analyzeFileListPath);
        System.out.println(tmpResultStoragePath);
        ExternalProcess.execute(projectFolder, "pmd.bat",
                "-filelist", analyzeFileListPath, "-f", "csv",
                "-R", "rulesets/java/basic.xml", ">", tmpResultStoragePath);
    }

    private List<PmdLineResult> getPmdResults() throws Exception{
        List<PmdLineResult> retList = new ArrayList<>();
        String[] headers = {"File", "Priority", "Line"};
        List<String[]> csvData = CsvOperationsUtil.getCSVData(tmpResultStoragePath,
                headers);
        for (String[] d: csvData){
            String filePath = d[0];
            String level = d[1];
            String lineNumber = d[2];
            PmdLineResult pr = new PmdLineResult();
            pr.setLevel(Integer.valueOf(level));
            pr.setLineNumber(Long.valueOf(lineNumber));
            String tmpFileName = new File(filePath).getName();
            int pathIndex = Integer.valueOf(tmpFileName.substring(0,
                    tmpFileName.indexOf(".java")));
            pr.setFilePath(pathsToAnalyze.get(pathIndex));
            retList.add(pr);
        }
        return retList;
    }

    private void writeFinalResultToCSV() throws Exception{
        List<PmdLineResult> results = getPmdResults();

        List<String[]> data = new ArrayList<>();
        for (PmdLineResult pr: results){
            data.add(pr.toCSVStrings());
        }

        String[] headers = PmdLineResult.getHeaders();
        CsvOperationsUtil.writeCSV(resultStoragePath, headers, data);
    }
}
