# Go-Back-N_PA2

## Program Description
Demonstrate a Go-Back-N Protocol using UDP sockets in Java. Using a provided emulated network link `emulator` that randomly drops packets to simulate an unreliable network. 
a makefile is included in this project.

### Makefile
to use the makefile, first run `make clean` to remove all unnecessary files. Then run `make` which compiles all java files into class files, which can then be run from the command line.

### Emulator Program (`emulatorDrop`)
Provided by Dr. Maxwell Young as a way to simulate lost/dropped packets. 
#### How to call the emulator
`java emulator <receivePort> <sendToClient-Port> <sendToServer-Port> <clientName> <serverName> <seed> <dropProb> <verbose-mode>`

| input  | Meaning |
| ------- | ------- |
|`<receivePort>` |emulator's receiving UDP port, used by both client and server |
|`<sendToClient-Port>`|client's receiving UDP port number |
|`<sendToServer-Port>`|server's receiving UDP port number |
|`<clientName>`|client's network address |
|`<serverName>`|server's network address |
|`<seed>`|used to seed the random generator |
|`<dropProb>`|packet discard probability |
|`<verbose-mode>`|int set to 1, network emulator output internal processing for debugging purposes|


#### Example of an emulator call
`java emulator 6000 6001 6002 localhost localhost 0 0.0 1`

### Packet class (`packet`)
A `packet` class is provided and must be used. No modifications allowed. 

Packet class has a  `type` member that specifies what type of packet it is. 

| `type`  | Meaning |
| ------- | ------- |
|`0`      |ACK      |
|`1`      |data Packet |
|`2`      |EOT from server to client |
|`3`      |EOT from client to server |

Packet class also has `seqnum` member that specifies the sequence number of the packet. This must be modulo `%` 8. 
Values of `seqnum` = `{0, 1, 2, 3, 4, 5, 6, 7}`

### Client program (`client`)
Made by students. Takes a text file and sends 30 char chunks of the data to the emulated network `emulator`. Utilizes the sliding door technique Go-Back-N to ensure reliable data transfer. 

#### GBN client side breakdown
* the client checks the window if it is full, if not send data packets. 
* timer of 2 seconds started
* Client receives ACK of sequence number (`seqnum`) *n* this is a cumulative ACK. all packets up to and including *n* have been received from the server.
  * send more packets until window full again. 
* Client timer expires, client resends ALL packets outstanding. 

The client produces 2 log files, `clientseqnum.log` and `clientack.log`. These files track the outgoing sequence numbers and incoming sequence numbers respectively. 

#### Command line input
`java client <emulatorAddress> <emulatorReceivingPort> <emulatorSendingPort> <filename>` 

| input  | Meaning |
| ------- | ------- |
|`<emulatorAddress>` |host address of the emulator      |
|`<emulatorReceivingPort>`|UDP port used by the emulator to receive data FROM the client |
|`<emulatorSendingPort>`|UDP port number used by the client to receive ACKs from the emulator |
|`<filename>`|name of the text file to be transferred |

#### Example of client call
`java client localhost 6000 6001 test.txt`

### Server program (`server`) 

#### Command line input
`java server <emulatorName> <receiveFromEmulator> <sendToEmulator-Port> <fileName>`

| input  | Meaning |
| ------- | ------- |
|`<emulatorName>` |host name of the emulator      |
|`<receiveFromEmulator>`|UDP port used by the server to receive data FROM the emulator |
|`<sendToEmulator-Port>`|UDP port number used by the emulator to receive ACKs from the server |
|`<filename>`|name of the text file which received data is written |

#### Example of server call 
`java server localhost 6002 6000 output.txt`

#### GBN explanation on server side

* check sequence number (`seqnum`)
* if sequence number is expected, send ACK with same sequence number and write `data` to `filename.txt`
* if sequence number is NOT expected, discard the packet. and resend ACK from most recent packet
* if server receives an EOT `type = 2` transmission from the client, server sends EOT ACK `type = 3` back to client. 

Server also writes to `arrival.log` which records sequence numbers of all data packets and the EOT packet from the client. 
