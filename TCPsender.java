public class TCPsender {
	private ArrayList<byte[]> datagrams;
	private short ackPort;
	private short sendPort;
	private String sendFileName;
	private String logFileName;
	private int windowSize;
	private DatagramSocket sendSocket;
	private ServerSocket ackSocket;
	private InetAddress receiverAddress;
	
}