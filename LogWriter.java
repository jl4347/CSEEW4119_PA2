import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Write log message to the specified log file
 */
public class LogWriter {
	private String filename;
	private PrintWriter fileWriter;
	private File logFile;

	public LogWriter() {
        this.filename = null;
        this.fileWriter = null;
        this.logFile = null;
    }

    public void setUp(String filename) throws IOException {
        logFile = new File(filename);
        if (filename.equals("stdout")) {
            fileWriter = new PrintWriter(System.out, true);
        } else {
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)), true);
        }
    }

    public void writeToLog(boolean isSender, String source, String destination,
            int seqNum, int ackNum, byte flag, long estimatedRTT, String type) {
        Date date = new Date();
        SimpleDateFormat dateFormat = setUpDateFormat();

        // Prepare ack and fin flags.
        int ack = 1;
        int fin = 0;

        // Check if the fin flag is set, since we are only using fin and ack.
        if (isFinSet(flag))
            fin = 1;

        // If it is the sender, then we include estimatedRTT.
        if (isSender) {
            this.fileWriter.println(prepareMessage(type,
                    dateFormat.format(date), source, destination, seqNum,
                    ackNum, ack, fin)
                    + ", Estimated RTT: " + estimatedRTT + " ms");
        } else {
            this.fileWriter.println(prepareMessage(type,
                    dateFormat.format(date), source, destination, seqNum,
                    ackNum, ack, fin));
        }
    }

    private SimpleDateFormat setUpDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
    }

    private boolean isFinSet(byte flag) {
        return flag == 17 ? true : false;
    }

    private String prepareMessage(String type, String date, String source,
            String destination, int seqNum, int ackNum, int ack, int fin) {
        return type + ": " + date + ", Source: " + source + ", Destination: "
                + destination + ", Sequence Number: " + seqNum
                + ", Ack Number: " + ackNum + ", Ack: " + ack + ", Fin: " + fin;
    }

    public void close() {
        this.fileWriter.close();
    }

	public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFileWriter(PrintWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    public String getFilename() {
        return this.filename;
    }

    public PrintWriter getFileWriter() {
        return this.fileWriter;
    }
}