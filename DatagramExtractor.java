import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.ArrayList;

public class DatagramExtractor {
    private final static int HEADER_SIZE = 20;
    private final static int INT_BYTE_SIZE = 4;
    private final static int PACKET_SIZE = 576;
    private final static int SHORT_BYTE_SIZE = 2;
    private ArrayList<byte[]> headers;
    private ArrayList<byte[]> messages;
    private ArrayList<byte[]> data;

    public DatagramExtractor() {
        this.headers = null;
        this.messages = null;
        this.data = null;
    }

    public void setUp() {
        this.headers = new ArrayList<byte[]>();
        this.messages = new ArrayList<byte[]>();
        this.data = new ArrayList<byte[]>();
    }

    public int retrieveOriginalSize(byte[] segment) {
        short sourcePort = extractSourcePortFromHeader(segment);
        short destPort = extractDestPortFromHeader(segment);
        short windowSize = extractWindowSizeFromHeader(segment);
        short checksumValue = extractChecksumFromHeader(segment);
        short urgentSize = extractUrgentFromHeader(segment);
        
        short inverseChecksum = (short) ~checksumValue;
        int originSize = inverseChecksum - (sourcePort + destPort + windowSize + urgentSize + HEADER_SIZE);
        return originSize;
    }

    public void extractMessagesFromSocket(DatagramSocket socket)
            throws IOException {
        byte[] receiveBuffer = new byte[PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(receiveBuffer,
                receiveBuffer.length);
        socket.receive(packet);
        receiveBuffer = packet.getData();
        this.messages.add(receiveBuffer);
    }

    public void extractHeaderFromMessage(byte[] message) {
        byte[] header = new byte[HEADER_SIZE];
        for (int i = 0; i < header.length; i++) {
            header[i] = message[i];
        }
        this.headers.add(header);
    }

    public byte[] extractDataFromMessage(byte[] message) {
        int dataSize = message.length - HEADER_SIZE;
        byte[] data = new byte[dataSize];
        System.arraycopy(message, 20, data, 0, dataSize);
        return data;
    }

    public short extractSourcePortFromHeader(byte[] header) {
        byte[] sourceByteArray = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(header, 0, sourceByteArray, 0, SHORT_BYTE_SIZE);
        return convertByteArrayToShort(sourceByteArray, ByteOrder.BIG_ENDIAN);
    }

    public short extractDestPortFromHeader(byte[] header) {
        byte[] destByteArray = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(header, 2, destByteArray, 0, SHORT_BYTE_SIZE);
        return convertByteArrayToShort(destByteArray, ByteOrder.BIG_ENDIAN);
    }

    public short extractWindowSizeFromHeader(byte[] header) {
        byte[] windowByteArray = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(header, 14, windowByteArray, 0, SHORT_BYTE_SIZE);
        return convertByteArrayToShort(windowByteArray, ByteOrder.BIG_ENDIAN);
    }

    public short extractChecksumFromHeader(byte[] header) {
        byte[] checksumArray = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(header, 16, checksumArray, 0, SHORT_BYTE_SIZE);
        return convertByteArrayToShort(checksumArray, ByteOrder.BIG_ENDIAN);
    }

    public short extractUrgentFromHeader(byte[] header) {
        byte[] urgentArray = new byte[SHORT_BYTE_SIZE];
        System.arraycopy(header, 18, urgentArray, 0, SHORT_BYTE_SIZE);
        return convertByteArrayToShort(urgentArray, ByteOrder.BIG_ENDIAN);
    }

    public boolean checkCheckSum(byte[] segment) {
        short segmentSize = new Integer(segment.length).shortValue();
        short sourcePort = extractSourcePortFromHeader(segment);
        short destPort = extractDestPortFromHeader(segment);
        short windowNum = extractWindowSizeFromHeader(segment);
        short checkNum = extractChecksumFromHeader(segment);
        short urgent = extractUrgentFromHeader(segment);

        int checksumValue = segmentSize + sourcePort + destPort + windowNum + urgent;
        short inverseCheckSum = (short) ~checksumValue;
        return inverseCheckSum == checkNum ? true : false;
    }

    private short convertByteArrayToShort(byte[] byteArray, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(order);
        return buffer.getShort();
    }

    public int extractSequenceNumberFromHeader(byte[] header) {
        byte[] sequenceNumber = new byte[INT_BYTE_SIZE];
        System.arraycopy(header, 4, sequenceNumber, 0, INT_BYTE_SIZE);
        return convertByteArrayToInt(sequenceNumber, ByteOrder.BIG_ENDIAN);
    }

    public int extractAckNumberFromHeader(byte[] header) {
        byte[] ackNumber = new byte[INT_BYTE_SIZE];
        System.arraycopy(header, 8, ackNumber, 0, INT_BYTE_SIZE);
        return convertByteArrayToInt(ackNumber, ByteOrder.BIG_ENDIAN);
    }

    public byte extractFlagsFromHeader(byte[] header) {
        return header[13];
    }

    private int convertByteArrayToInt(byte[] byteArray, ByteOrder order) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(order);
        return buffer.getInt();
    }

    public void setHeaders(ArrayList<byte[]> headers) {
        this.headers = headers;
    }

    public void setMessages(ArrayList<byte[]> messages) {
        this.messages = messages;
    }

    public void setData(ArrayList<byte[]> data) {
        this.data = data;
    }

    public ArrayList<byte[]> getHeaders() {
        return this.headers;
    }

    public ArrayList<byte[]> getMessages() {
        return this.messages;
    }

    public ArrayList<byte[]> getData() {
        return this.data;
    }
}