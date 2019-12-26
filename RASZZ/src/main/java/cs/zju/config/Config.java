package cs.zju.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    private static final Configuration config = getProjectConfiguration();
    private static final String[] projects = getStringArrayProperty("projects");
    private static final String[] cloneUrls = getStringArrayProperty("urls");

    public static Configuration getProjectConfiguration(){
        Configurations configs = new Configurations();
        try {
            return configs.properties(new File("config.properties"));
        } catch (ConfigurationException ex) {
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String[] getStringArrayProperty(String propertyName){
        return config.getStringArray(propertyName);
    }

    public static String getStringProperty(String propertyName){
        return config.getString(propertyName);
    }

    public static boolean getBooleanProperty(String propertyName){
        return config.getBoolean(propertyName);
    }

    public static List<String> getProjects(){
        return new ArrayList<>(Arrays.asList(projects));
    }

    public static List<String> getCloneUrls(){
        return new ArrayList<>(Arrays.asList(cloneUrls));
    }

    public static String projectPath(String project){
        String tmpFolder = getStringProperty("tmpfolder");
        return new File(new File(tmpFolder), "gitfiles/" + project).getAbsolutePath();
    }

    public static String projectPmdPath(String project){
        String path = projectPath(project);
        File baseDir = new File(path).getParentFile();
        File pmdDir = new File(baseDir, "pmd/" + project);
        if (!pmdDir.exists())
            pmdDir.mkdirs();
        return pmdDir.getAbsolutePath();
    }

    public static String projectPmdResultPath(String project, String commitId){
        String pmdPath = projectPmdPath(project);
        return new File(new File(pmdPath), "result-"+commitId+".csv").getAbsolutePath();
    }

    public static String resultPath(String project){
        String path = projectPath(project);
        File baseDir = new File(path).getParentFile();
        File resultFile = new File(baseDir, project + "_add.csv");
        return resultFile.getAbsolutePath();
    }
}
