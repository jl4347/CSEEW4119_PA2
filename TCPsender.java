import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.Date;

public class TCPsender {
	private static ArrayList<byte[]> datagrams;
	private short ackPort;
	private short receiverPort;
	private String sendFileName;
	private String logFileName;
	private static int windowSize;
	private static DatagramSocket sendSocket;
	private ServerSocket ackSocket;
	private InetAddress receiverAddress;
	private static long timeout;
	private static long estimatedRTT;
	private static int sequenceRange;
	private static int totalBytesSent;
	private static int totalSegmentsSent;
	private static int retransmissions;
	private static LogWriter writer;

	private static int sendBase;
	private static int nextSequence;
	private static boolean baseACK;

	private final static int SEQ_NUM_INDEX = 4;
    private final static int ACK_NUM_INDEX = 8;
    private final static int FLAG_INDEX = 13;
    private final static int INT_BYTE_SIZE = 4;

	public static void main(String[] args) {
		if (args.length != 5 && args.length != 6)
			printInstructions();
		else runSender(args);
	}
	
	private static void printInstructions() {
		System.out.println("java TCPsender <filename> <remote_IP> <remote_port>"
                		 + " <ack_port_num> <log_filename> <window_size>\n"
                		 + "<window_size> default 1");
		System.exit(1);
	}

	public static class GBNProtocol extends Thread {
		private static long startTime;
		private static long currTime;
		private TCPsender sender;

		public GBNProtocol(TCPsender sender) {
			this.startTime = 0;
			this.currTime = 0;
			this.sender = sender;
		}

		@Override
		public void run() {
			while (sendBase < datagrams.size()) {
				startTime = System.currentTimeMillis();
				baseACK = false;

				while (nextSequence < sendBase + windowSize && nextSequence < datagrams.size()) {
					byte[] datagram = datagrams.get(nextSequence);
					DatagramPacket packet = new DatagramPacket(datagram, datagram.length, 
						sender.getReceiverAddress(), sender.getReceiverPort());
					// send packet
					try {
						sender.sendPacket(packet);
					} catch (IOException e) {
						e.printStackTrace();
					}
					totalBytesSent += datagram.length;

					// Write the log file
					sender.writeSentMessage(datagram, writer);

					nextSequence++;
				}

				// Retransmission if timer expires
				while (sendBase < datagrams.size() && !baseACK) {
					currTime = System.currentTimeMillis();
					if (currTime - startTime >= timeout) {
						retransmissions += nextSequence - sendBase;
						nextSequence = sendBase;
						break;
					}
				}
			}
		}
	}
	
	private static void runSender(String[] args) {
		TCPsender sender = new TCPsender();
		writer = new LogWriter();
		try {
			sender.setUp(args);
			setUpCounters();

			// Instantiate the datagram generator
			DatagramGenerator datagramGenerator = new DatagramGenerator();
			// Setup the log writer
			writer.setUp(sender.getLogFileName());
			// Generate all the datagrams
			datagrams = datagramGenerator.generateDatagram(sender.getSendPort(), sender.getReceiverPort(),
				sender.getSequenceRange(), sender.getSendFileName());

			


		} catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }

        System.exit(1);
	}

	private int extractIntFromHeader(byte[] header, int index) {
        byte[] temp = new byte[4];
        System.arraycopy(header, index, temp, 0, INT_BYTE_SIZE);
        int value = ByteBuffer.wrap(temp).getInt();
        return value;
    }

	private void writeSentMessage(byte[] segment, LogWriter writer) {
        int seqNumber = this.extractIntFromHeader(segment, SEQ_NUM_INDEX);
        int ackNumber = this.extractIntFromHeader(segment, ACK_NUM_INDEX);
        String senderAddress = this.getSendSocket().getLocalSocketAddress()
                .toString();
        String receiverAddress = this.getReceiverAddress().toString() + ":"
                + this.getReceiverPort();
        writer.writeToLog(false, senderAddress, receiverAddress.substring(1),
                seqNumber, ackNumber, segment[FLAG_INDEX], this.estimatedRTT, "Sent");
    }
	
	public TCPsender() {
		this.ackPort = 0;
		this.receiverPort = 0;
		this.sendFileName = null;
		this.logFileName = null;
		this.windowSize = 1;
		this.sequenceRange = 2;
		this.sendSocket = null;
		this.ackSocket = null;
		this.receiverAddress = null;
		this.timeout = 0;
		this.estimatedRTT = 0;

		this.sendBase = 0;
		this.nextSequence = 0;
		this.baseACK = false;
	}

	private void setUp(String[] args) throws UnknownHostException, IOException {
		this.setAckPort(Short.parseShort(args[3]));
		this.setReceiverPort(Short.parseShort(args[2]));
		this.setSendFileName(args[0]);
		this.setLogFileName(args[4]);
		if (args.length == 6) this.setWindowSize(Integer.parseInt(args[5]));
		this.sequenceRange = this.windowSize * 2;
		this.setSendSocket();
		this.setAckSocket(this.getAckPort());
		this.setReceiverAddress(InetAddress.getByName(args[1]));
		this.setTimeOut(1000);
		this.estimatedRTT = 1000;
	}

	private static void setUpCounters() {
		retransmissions = 0;
        totalSegmentsSent = 0;
        totalBytesSent = 0;
	}

	private void sendPacket(DatagramPacket packet) throws IOException {
        this.getSendSocket().send(packet);
        totalSegmentsSent++;
    }

	// Setters
	public void setAckPort(short ackPort) {
		this.ackPort = ackPort;
	}

	public void setReceiverPort(short receiverPort) {
		this.receiverPort = receiverPort;
	}

	public void setSendFileName(String sendFileName) {
		this.sendFileName = sendFileName;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public void setSequenceRange(int sequenceRange) {
		this.sequenceRange = sequenceRange;
	}

	public void setSendSocket() throws SocketException {
		this.sendSocket = new DatagramSocket();
	}

	public void setAckSocket(short ackPort) throws IOException {
		this.ackSocket = new ServerSocket(ackPort);
	}

	public void setReceiverAddress(InetAddress receiverAddress) {
		this.receiverAddress = receiverAddress;
	}

	public void setTimeOut(long timeout) {
		this.timeout = timeout;
	}

	// Getters
	public short getAckPort() {
		return this.ackPort;
	}

	public short getReceiverPort() {
		return this.receiverPort;
	}

	public String getSendFileName() {
		return this.sendFileName;
	}

	public String getLogFileName() {
		return this.logFileName;
	}

	public int getWindowSize() {
		return this.windowSize;
	}

	public int getSequenceRange() {
		return this.sequenceRange;
	}

	public DatagramSocket getSendSocket() {
		return this.sendSocket;
	}

	public ServerSocket getAckSocket(){
		return this.ackSocket;
	}

	public InetAddress getReceiverAddress() {
		return this.receiverAddress;
	}

	public short getSendPort() {
        Integer sendPort = this.getSendSocket().getLocalPort();
        short shortNumber = sendPort.shortValue();
        return shortNumber;
    }

	public long getTimeOut() {
		return this.timeout;
	}
}