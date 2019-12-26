package core.connector.util;

import java.io.*;

public class ExternalProcess {

    public static String execute(File workingDir, String ... commandAndArgs) {
        try {
            Process p = new ProcessBuilder(commandAndArgs)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start();
            try {
                StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
                outputGobbler.run();
                //Thread outputGobblerThread = new Thread(outputGobbler);
                //outputGobblerThread.start();
                p.waitFor();

                if (p.exitValue() == 0) {
                    return outputGobbler.getOutput();
                } else {
                    if (outputGobbler.getOutput().trim().endsWith("did not match any file(s) known to git"))
                        throw new ExternalExeException(outputGobbler.getOutput());
//                    else
//                        throw new RuntimeException("Execution error: " + outputGobbler.getOutput());
                    System.out.println(p.exitValue());
                    return outputGobbler.getOutput();
                }
            }
            finally {
                close(p.getInputStream());
                close(p.getOutputStream());
                //p.destroy();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error executing command " + commandAndArgs, e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error executing command " + commandAndArgs, e);
        }
    }

    private static void close(Closeable closeable) throws IOException {
        if (closeable != null) {
            closeable.close();
        }
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream is;
        private final StringBuffer output = new StringBuffer();

        StreamGobbler(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null) {
                    output.append(line + '\n');
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getOutput() {
            return this.output.toString();
        }
    }
}
