import java.io.*;
import java.net.*;

public class TCPreceiver {
    private short listeningPort;
    private short senderPort;
    private InetAddress senderAddress;
    private String logFilename;
    private String receivedFilename;
    private Socket ackSocket;
    private DatagramSocket listenSocket;
    private final static int MSS = 576;
    private int ackNumber;
    private int sequenceNumber;
    private int sequenceRange;

    public static void main(String[] args) {
        if (args.length != 5) {
            printArguments();
        } else {
            runReceiver(args);
        }
    }

    private String getRecieveSource() {
        return this.getAckSocket().getLocalSocketAddress().toString().substring(1);
    }

    private String getReceiveDest() {
        return this.getSenderAddress().toString().substring(1) + ":" + this.getSenderPort();
    }

    private static void printArguments() {
        System.out.println("java TCPreceiver <filename> <listening_port>"
                + " <sender_IP> <sender_port> <log_filename>");
        System.exit(1);
    }

    private static void runReceiver(String[] args) {
        TCPreceiver receiver = new TCPreceiver();
        LogWriter writer = new LogWriter();

        try {
            // Set up the receiver.
            receiver.setUp(args);
            DatagramExtractor extractor = new DatagramExtractor();
            writer.setUp(receiver.getLogFilename());
            BufferedWriter socketWriter = null;

            while (true) {

                // Create byte array for the next incoming packet.
                byte[] incomingSegment = new byte[MSS];
                DatagramPacket packet = new DatagramPacket(incomingSegment,
                        incomingSegment.length);

                // Receive the packet.
                receiver.getListenSocket().receive(packet);

                // Retrieve the data.
                incomingSegment = packet.getData();

                int originalSize = extractor.retrieveOriginalSize(incomingSegment) + 20;

                if (originalSize < 0) {
                    continue;
                }

                byte[] correctSegment = new byte[originalSize];
                try {
                    System.arraycopy(incomingSegment, 0, correctSegment, 0, originalSize);
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

                // Set all the variables.
                String sourceAddress = receiver.getSourceAddress();
                String destinationAddress = receiver.getDestAddress();
                int seqNum = extractor.extractSequenceNumberFromHeader(correctSegment);
                int ackNum = extractor.extractAckNumberFromHeader(correctSegment);
                byte flag = extractor.extractFlagsFromHeader(correctSegment);
                short windowSize = extractor.extractWindowSizeFromHeader(correctSegment);

                // Write to log file.
                writer.writeToLog(false, sourceAddress, destinationAddress, 
                    seqNum, ackNum, flag, 0, "Received");

                if (receiver.sequenceNumbersMatch(seqNum) && extractor.checkCheckSum(correctSegment)) {
                    receiver.setSequenceRange((int)windowSize * 2);
                    byte[] data = extractor.extractDataFromMessage(correctSegment);
                    receiver.writeByteArrayToFile(data);

                    int updatedSeq = receiver.updateSeqNumber();
                    receiver.setSequenceNumber(updatedSeq);
                    System.out.println("next expected seqNum: " + receiver.getSequenceNumber());
                    if (receiver.getAckSocket() == null)
                        receiver.setAckSocket(receiver.getSenderAddress(), receiver.getSenderPort());
                        socketWriter = receiver.createSockOut();
                    receiver.writeResponse(updatedSeq, ackNum, flag, socketWriter);

                    receiver.writeSentLog(updatedSeq, ackNum, flag, writer);
                    if (receiver.isFinOn(flag)) {
                        break;
                    }
                }
            }
        } catch (UnknownHostException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        } catch (SocketException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
        System.out.println("Delivery completed successfully");
        writer.close();
        receiver.getListenSocket().close();
    }

    private void writeSentLog(int updateSeq, int ackNum, byte flag,
            LogWriter writer) {
        String receiveSource = this.getRecieveSource();
        String receiveDest = this.getReceiveDest();
        writer.writeToLog(false, receiveSource, receiveDest, updateSeq, ackNum,
                flag, 0, "Sent");
    }

    private String getSourceAddress() {
        return getSenderAddress().getHostAddress() + ":" + getSenderPort();
    }

    private String getDestAddress() {
        return getListenSocket().getLocalAddress() + ":" + getListeningPort();
    }

    private boolean sequenceNumbersMatch(int seqNum) {
        return this.getSequenceNumber() == seqNum;
    }

    private void writeByteArrayToFile(byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(this.getReceivedFilename(), true);
        fos.write(data);
        fos.close();
    }

    private int updateSeqNumber() {
        int seqNum = (this.getSequenceNumber() + 1) % this.getSequenceRange();
        return seqNum;
    }

    private BufferedWriter createSockOut() throws IOException {
        return new BufferedWriter(new OutputStreamWriter(this.getAckSocket()
                .getOutputStream()));
    }

    private void writeResponse(int seqNum, int ackNum, byte flag,
            BufferedWriter socketWriter) throws IOException {
        String response = "SEQ " + seqNum + " ACK " + ackNum + " FLAG " + flag + "\n";
        System.out.println(response);
        socketWriter.write(response);
        socketWriter.flush();
    }

    private boolean isFinOn(byte flags) {
        return flags == 17 ? true : false;
    }

    public TCPreceiver() {
        this.listeningPort = 0;
        this.senderPort = 0;
        this.logFilename = null;
        this.receivedFilename = null;
        this.senderAddress = null;
        this.ackSocket = null;
        this.listenSocket = null;
        this.ackNumber = 0;
        this.sequenceNumber = 0;
        this.sequenceRange = 0;
    }

    public void setUp(String[] args) throws UnknownHostException,
            SocketException, IOException {
        this.setReceivedFilename(args[0]);
        this.setListeningPort(Short.parseShort(args[1]));
        this.setSenderAddress(InetAddress.getByName(args[2]));
        this.setSenderPort(Short.parseShort(args[3]));
        this.setLogFilename(args[4]);
        this.setListenSocket(this.getListeningPort());
        this.setAckNumber(0);
        this.setSequenceNumber(0);
        this.setSequenceRange(2);
    }

    public void setListeningPort(short listeningPort) {
        this.listeningPort = listeningPort;
    }

    public void setSenderPort(short senderPort) {
        this.senderPort = senderPort;
    }

    public void setLogFilename(String logFilename) {
        this.logFilename = logFilename;
    }

    public void setReceivedFilename(String receivedFilename) {
        this.receivedFilename = receivedFilename;
    }

    public void setSenderAddress(InetAddress senderAddress) {
        this.senderAddress = senderAddress;
    }

    public void setListenSocket(short listenPortNumber) throws SocketException {
        this.listenSocket = new DatagramSocket(listenPortNumber);
    }

    public void setAckSocket(InetAddress senderAddress, short senderPortNumber)
            throws IOException {
        this.ackSocket = new Socket(senderAddress, senderPortNumber);
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public void setSequenceRange(int sequenceRange) {
        this.sequenceRange = sequenceRange;
    }

    public short getListeningPort() {
        return this.listeningPort;
    }

    public short getSenderPort() {
        return this.senderPort;
    }

    public String getLogFilename() {
        return this.logFilename;
    }

    public String getReceivedFilename() {
        return this.receivedFilename;
    }

    public InetAddress getSenderAddress() {
        return this.senderAddress;
    }

    public DatagramSocket getListenSocket() {
        return this.listenSocket;
    }

    public Socket getAckSocket() {
        return this.ackSocket;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public int getAcKNumber() {
        return this.ackNumber;
    }

    public int getSequenceRange() {
        return this.sequenceRange;
    }
}