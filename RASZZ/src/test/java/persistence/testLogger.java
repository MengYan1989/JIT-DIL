package persistence;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Properties;

public class testLogger {
    public static void main(String[] args) throws Exception{
        BasicConfigurator.configure();
        Logger logger = Logger.getLogger(testLogger.class);
        String log4jFile = "log4j.properties";
        Properties p = new Properties();

        p.load(new FileInputStream(new File(log4jFile)));
        PropertyConfigurator.configure(p);
        logger.info("Wow");
    }
}
