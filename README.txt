Programming Assignment 2: Simple TCP­like transport­layer protocol

UNI: jl4347
Name: Jialun Liu

=======================================================================================
How to Use
=======================================================================================
In order to build the application first, run the following command:
$ make all

Then run TCPreceiver and TCPsender:
java TCPreceiver <filename> <listening_port> <sender_IP> <sender_port> <log_filename>

java TCPsender <filename> <remote_IP> <remote_port> <ack_port_num> <log_filename> 
<window_size>

The default window size of TCP sender is 1.

Sample command lines for receiver and sender:
java TCPreceiver test.txt 20002 192.168.0.2 20003 stdout

java TCPsender sample.txt 192.168.0.2 20002 20003 stdout 100

=======================================================================================
Program Structure
=======================================================================================
1. TCPreceiver.java:
Receive the list of the datagrams for the specified file, designed to deal with packet loss,
corruption, duplication and reordering.

All those all achieved by implementing the Go-Back-N protocol.

2. TCPsender.java:
Send the list of the datagrams to the specified receiver Designed to deal with packet loss,
corruption, duplication and reordering.

Using the GBN protocol to provide a pipeline implementation.

3. DatagramGenerator.java:
Generate the list of datagrams from the specfied file for transmission.

4. DatagramExtractor.java:
Extract information from the received datagram, eg, header, flag, data...

5. LogWriter.java:
Write log messages to the specified log file.

=======================================================================================
TCP segment structure
=======================================================================================
The datagram header includes the following information:
1. source port
2. destination port
3. sequence number
4. acknowleddgment number
5. Dataoffset
6. Reserved
7. ACK and FIN flag
8. Window Size
9. Checksum
10. Urgent Pointer
11. Actual Data

=======================================================================================
Loss Recovery Mechanism
=======================================================================================
The TCP sender and receiver implements the Go-Back-N protocol as its loss recovery mechanism.

The sender maintains a send window which has size specified by user, and the window is
aligned with the receiver window.

Sender maintains two variables "sendBase" and "nextSequenceNumber" to maintain the window,
and the window would only move forward if the received ACK from receiver matches with
"sendBase". The sender ends when all the datagrams are sent and ACKed.

Receiver uses only one variable "sequenceNumber" to keep track of the next expected datagram.
It only acks back to sender when the received datagram has the sequence number matching the
"sequenceNumber". The receiver ends when it has received the last datagram of the file.

The difference between the GBN protocol described in book and my implementation is the
receiver, it will not ack back the last correctly received ACK when it receives a out of 
order packet.