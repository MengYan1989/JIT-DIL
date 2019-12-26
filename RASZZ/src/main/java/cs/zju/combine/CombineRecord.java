package cs.zju.combine;

import java.util.Date;

public class CombineRecord {
    private String commitHash;
    private String content;
    private String preFilePath;
    private String newFilePath;
    private long lineNumber;
    private String author;
    private Date time;
    private boolean bugIntroducing;
    private boolean commitLabel;
    private int pmdLevel;

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPreFilePath() {
        return preFilePath;
    }

    public void setPreFilePath(String preFilePath) {
        this.preFilePath = preFilePath;
    }

    public String getNewFilePath() {
        return newFilePath;
    }

    public void setNewFilePath(String newFilePath) {
        this.newFilePath = newFilePath;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public boolean isBugIntroducing() {
        return bugIntroducing;
    }

    public void setBugIntroducing(boolean bugIntroducing) {
        this.bugIntroducing = bugIntroducing;
    }

    public boolean getCommitLabel() {
        return commitLabel;
    }

    public void setCommitLabel(boolean commitLabel) {
        this.commitLabel = commitLabel;
    }

    public int getPmdLevel() {
        return pmdLevel;
    }

    public void setPmdLevel(int pmdLevel) {
        this.pmdLevel = pmdLevel;
    }

    public static String[] getHeaders(){
        String[] headers = {"commit_hash", "content", "file_pre", "file_new",
        "line_num", "author", "time", "bug_introducing", "commit_label", "pmd"};

        return headers;
    }

    public String[] toCsvRecord(){
        String[] record = new String[10];
        record[0] = this.getCommitHash();
        record[1] = this.getContent();
        record[2] = this.getPreFilePath();
        record[3] = this.getNewFilePath();
        record[4] = String.valueOf(this.getLineNumber());
        record[5] = this.getAuthor();
        record[6] = this.getTime().toString();
        record[7] = String.valueOf(this.isBugIntroducing());
        record[8] = String.valueOf(this.getCommitLabel());
        record[9] = String.valueOf(this.getPmdLevel());
        return record;
    }
}
