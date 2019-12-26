package br.ufrn.raszz.model.szz;

import java.util.*;

public class PmdRevisionResult {
    private String revision;
    private List<PmdLineResult> lineResults;

    public PmdRevisionResult(){
        lineResults = new ArrayList<>();
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public List<PmdLineResult> getLineResults() {
        return lineResults;
    }

    public void setLineResults(List<PmdLineResult> lineResults) {
        this.lineResults = lineResults;
    }

    public static class PmdLineResultMap{
        private Map<Long, Integer> lineLevelMap = new HashMap<>();

        public void putValue(Long line, Integer level){
            lineLevelMap.put(line, level);
        }

        public int getLineLevel(Long line){
            return lineLevelMap.getOrDefault(line, 0);
        }
    }

    public static class PmdFileResultMap {
        private Map<String, PmdLineResultMap> fileMap = new HashMap<>();

        public void fromPmdRevisionResult(PmdRevisionResult prr){
            if (prr.getLineResults() == null){
                return;
            }
            for (PmdLineResult pr: prr.getLineResults()){
                String filePath = pr.getFilePath();
                long lineNumber = pr.getLineNumber();
                int level = pr.getLevel();
                if (!fileMap.containsKey(filePath))
                    fileMap.put(filePath, new PmdLineResultMap());
                fileMap.get(filePath).putValue(lineNumber, level);
            }
        }

        public int getLineLevel(String filePath, long lineNumber){
            if (!fileMap.containsKey(filePath))
                return 0;
            return fileMap.get(filePath).getLineLevel(lineNumber);
        }
    }
}

