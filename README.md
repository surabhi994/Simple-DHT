# Simple-DHT
Simple DHT is an Android Application that implements Distributed Hash Table. A distributed Hash Table is a key value storage which is based on the CHORD protocol. 

In the CHORD protocol, all the nodes are arranged in a ring and each node has a successor and a predeccessor. Chord covers three things:
-ID space partitioning/re-partitioning.
-Ring based routing
-Node joins
 
 All node joins are handled in realtime and position in the rings are alloted by the first avd i.e avd0.
 
 SHA-1 hash function is used to lexically arrange nodes in a ring and find the location for a particular key to be stored.
 
 Content Provider is a key value storage which uses SharedPreference of Android to store the key-value.
