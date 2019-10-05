# Go-Back-N_PA2

## Program Description
Demonstrate a Go-Back-N Protocol using UDP sockets in Java. Using a provided emulated network link `emulator` that randomly drops packets to simulate an unreliable network. 
a makefile is included in this project. To use the makefile _**TODO**_

### Emulator Program (`emulator`)
Provided by Dr. Maxwell Young as a way to simulate lost/dropped packets. 
#### How to call the emulator
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
To use type 

`java client <emulatorAddress> <emulatorReceivingPort> <emulatorSendingPort> <filename>` 

| input  | Meaning |
| ------- | ------- |
|`<emulatorAddress>` |host address of the emulator      |
|`<emulatorReceivingPort>`|UDP port used by the emulator to receive data FROM the client |
|`<emulatorSendingPort>`|UDP port number used by the client to receive ACKs from the emulator |
|`<filename>`|name of the text file to be transferred |

#### Example of client call
`java client localhost 6000 6001 test.txt`

### Server program (`server`) _*TODO*_


