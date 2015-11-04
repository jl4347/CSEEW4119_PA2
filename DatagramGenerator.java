import java.io.*;
import java.nio.*;
import java.util.ArrayList;

public class DatagramGenerator {
	private final static int SHORT_BYTE_SIZE = 2;
    private final static int INT_BYTE_SIZE = 4;
    private final static int HEADER_BYTE_SIZE = 20;
    private final byte ackFinFlag = 17;
    private final byte ackFlag = 16;
    private final int MSS = 556;
    private final byte[] urgentPointer = new byte[2];
    private final byte dataOffsetReservedNS = 80;
    private ArrayList<byte[]> segments;
    private ArrayList<byte[]> datagrams;
    private ArrayList<byte[]> headers;
    private int sequenceRange;
    private int seqNum;
    private int ackNum;

    public DatagramGenerator() {
    	this.segments = null;
        this.datagrams = null;
        this.headers = null;
        this.sequenceRange = 2;
        this.seqNum = 0;
        this.ackNum = 0;
    }

    public ArrayList<byte[]> generateDatagram(short sourcePort, short destinationPort, 
    	int sequenceRange, String filename) throws IOException {
    	this.sequenceRange = sequenceRange;
    	byte[] fileBytes = convertFileToByte(filename);
    	this.segments = convertFileToSegments(fileBytes);
    	generateTCPsegments(sourcePort, destinationPort);
    	return this.datagrams;
    }

    private byte[] convertFileToByte(String filename) throws IOException {
    	File file = new File(filename);
    	FileInputStream fis = new FileInputStream(file);
    	byte[] data = new byte[(int)file.length()];
    	fis.read(data);
    	fis.close();
    	return data;
    }

    private ArrayList<byte[]> convertFileToSegments(byte[] file) {
    	ArrayList<byte[]> segments = new ArrayList<byte[]>();
    	byte[] segment = new byte[MSS];
    	for (int i = 0; i < file.length; i++) {
    		if (i % MSS == 0) {
    			segments.add(segment);
    			segment = new byte[MSS];
    		}
    		segment[i % MSS] = file[i];
    	}
    	segments.remove(0);
    	byte[] lastSegment = new byte[file.length % MSS];
    	System.arraycopy(file, segments.size() * MSS, lastSegment, 0, lastSegment.length);
    	segments.add(lastSegment);
    	return segments;
    }

    private void generateTCPsegments(short sourcePort, short destinationPort) {
    	generateTCPheaders(sourcePort, destinationPort);
    	combineHeadersAndSegments();
    }

    private void generateTCPheaders(short sourcePort, short destinationPort) {
    	this.headers = new ArrayList<byte[]>();
    	int lastHeader = this.segments.size() - 1;

    	byte[] source = convertShortToByte(sourcePort, ByteOrder.BIG_ENDIAN);
    	byte[] destination = convertShortToByte(destinationPort, ByteOrder.BIG_ENDIAN);
    	byte[] windowSize = convertIntToByte(this.sequenceRange / 2, ByteOrder.BIG_ENDIAN);
    	for (int i = 0; i < this.segments.size(); i++) {
    		byte[] header = new byte[HEADER_BYTE_SIZE];
    		byte[] seqNumber = convertIntToByte(this.seqNum, ByteOrder.BIG_ENDIAN);
            byte[] ackNumber = convertIntToByte(this.ackNum, ByteOrder.BIG_ENDIAN);
            if (i == lastHeader)
            	header[13] = ackFinFlag;
           	else header[13] = ackFlag;

           	this.headers.add(header);
           	this.seqNum = (this.seqNum + 1) % this.sequenceRange;
           	this.ackNum = (this.ackNum + 1) % this.sequenceRange;
    	}
    }

    private byte[] convertShortToByte(short value, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.allocate(SHORT_BYTE_SIZE);
        buffer.order(order);
        return buffer.putShort(value).array();
    }

    private byte[] convertIntToByte(int value, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.allocate(INT_BYTE_SIZE);
        buffer.order(order);
        return buffer.putInt(value).array();
    }

    private void setTCPheader(byte[] header, byte[] source, byte[] destination, 
    	byte[] seqNumber, byte[] ackNumber, byte[] windowSize, byte[] segment) {
    	System.arraycopy(source, 0, header, 0, SHORT_BYTE_SIZE);
        System.arraycopy(destination, 0, header, 2, SHORT_BYTE_SIZE);
        System.arraycopy(seqNumber, 0, header, 4, INT_BYTE_SIZE);
        System.arraycopy(ackNumber, 0, header, 8, INT_BYTE_SIZE);
        header[12] = dataOffsetReservedNS;
        System.arraycopy(windowSize, 0, header, 14, SHORT_BYTE_SIZE);
        System.arraycopy(urgentPointer, 0, header, 18, SHORT_BYTE_SIZE);
    }

    private void combineHeadersAndSegments() {
    	this.datagrams = new ArrayList<byte[]>();
        for (int i = 0; i < this.headers.size(); i++) {
            byte[] header = this.headers.get(i);
            byte[] data = this.segments.get(i);
            byte[] tcpScrap = new byte[header.length + data.length];
            System.arraycopy(header, 0, tcpTemp, 0, header.length);
            System.arraycopy(data, 0, tcpTemp, header.length, data.length);

            byte[] tcpMessage = calculateCheckSum(tcpScrap);
            this.datagrams.add(tcpMessage);
        }
    }

    private byte[] calculateCheckSum(byte[] message) {
     	byte[] sourceNumber = new byte[SHORT_BYTE_SIZE];
        byte[] destNumber = new byte[SHORT_BYTE_SIZE];
        byte[] windowSize = new byte[SHORT_BYTE_SIZE];
       	
        System.arraycopy(message, 0, sourceNumber, 0, SHORT_BYTE_SIZE);
        System.arraycopy(message, 2, destNumber, 0, SHORT_BYTE_SIZE);
        System.arraycopy(message, 14, windowSize, 0, SHORT_BYTE_SIZE);
        
        short segmentSize = new Integer(message.length).shortValue();
        short sourcePort = convertByteArrayToShort(sourceNumber, ByteOrder.BIG_ENDIAN);
        short destPort = convertByteArrayToShort(destNumber, ByteOrder.BIG_ENDIAN);
        short windowNum = convertByteArrayToShort(windowSize, ByteOrder.BIG_ENDIAN);

        int checksumValue = segmentSize + sourcePort + destPort + windowNum;

        short checkShort = new Integer(checksumValue).shortValue();
        short inverse = (short) ~checkShort;

        byte[] checkSum = convertShortToByte(inverse, ByteOrder.BIG_ENDIAN);

        System.arraycopy(checkSum, 0, message, 16, SHORT_BYTE_SIZE);
        return message;
    }

    private short convertByteArrayToShort(byte[] byteArray, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        return buffer.order(order).getShort();
    }

    public void setSegments(ArrayList<byte[]> segments) {
        this.segments = segments;
    }

	public void setDatagrams(ArrayList<byte[]> messages) {
        this.datagrams = datagrams;
    }

    public void setHeaders(ArrayList<byte[]> headers) {
        this.headers = headers;
    }

    public void setSequenceRange(int sequenceRange) {
        this.sequenceRange = sequenceRange;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public ArrayList<byte[]> getSegments() {
        return this.segments;
    }

	public ArrayList<byte[]> getDatagrams() {
        return this.datagrams;
    }

    public ArrayList<byte[]> getHeaders() {
        return this.headers;
    }

    public int getSequenceRange(int sequenceRange) {
        return this.sequenceRange;
    }

    public int getSeqNum() {
        return this.seqNum;
    }

    public int getAckNum() {
        return this.ackNum;
    }
}