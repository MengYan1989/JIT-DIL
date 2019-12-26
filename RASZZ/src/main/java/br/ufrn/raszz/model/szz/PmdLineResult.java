package br.ufrn.raszz.model.szz;

import br.ufrn.raszz.util.CsvOperationsUtil;
import cs.zju.config.Config;

import java.util.ArrayList;
import java.util.List;

public class PmdLineResult {
    private int level;
    private long lineNumber;
    private String filePath;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public static String[] getHeaders(){
        String[] ret = {"filePath", "lineNumber", "level"};
        return ret;
    }

    public String[] toCSVStrings(){
        String[] ret = new String[3];
        ret[0] = filePath;
        ret[1] = String.valueOf(lineNumber);
        ret[2] = String.valueOf(level);
        return ret;
    }

    public static List<PmdLineResult> getPmdLineResultFromCSV(String project, String revision)throws Exception{
        String pmdStoragePath = Config.projectPmdResultPath(project, revision);
        List<PmdLineResult> ret = new ArrayList<>();
        String[] headers = PmdLineResult.getHeaders();
        List<String[]> csvData = CsvOperationsUtil.getCSVData(pmdStoragePath, headers);
        if (csvData == null || csvData.size()==0) return null;
        for (String[] d: csvData){
            PmdLineResult pr = new PmdLineResult();
            pr.setFilePath(d[0]);
            pr.setLineNumber(Long.valueOf(d[1]));
            pr.setLevel(Integer.valueOf(d[2]));
            ret.add(pr);
        }
        return ret;
    }

}
