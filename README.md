# ics460_gp2

Project Description:

This project provides an all-inclusive implementation experience of major topics in Computer Networks, including protocol design and implementation.
The User Datagram Protocol (UDP) provides point-to-point, unreliable datagram service between a pair of hosts. It does not provide any reliability or sequencing guarantees – i.e. packets may arrive late, may not arrive at all, or arrive corrupted. 
Your project is to implement a reliable and sequenced message transport protocol on top of this unreliable UDP (stop and wait). Your protocol will ensure reliable, end-to-end delivery of messages in the face of packet loss, and will preserve message ordering in the face of arbitrary latencies due to multiple paths taken by packets.
In order to simulate a lossy network on a single computer, you will implement a proxy that randomly drops, delays, or corrupts packets.
Whereas TCP allows fully bidirectional communication, your implementation will be asymmetric. Each endpoint will play the role of a "sender" and a "receiver". Data packets will only flow from the sender to the receiver, while ACKs will only flow in the "reverse" direction from the receiver back to the sender. 
(Project inspired by my project assignment at UMN and more recently by Stefan Savage, UC San Diego.)

We had to present the project with working output and code for verification.

My contribution and collaboration was done on my other Andrew Account.
