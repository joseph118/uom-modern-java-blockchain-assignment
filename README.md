# README - LAS3006 - Java extreme development

A java 8 application containing 3 modules which holds a client application
 called the wallet, and another server application called the node. The
  other module is a common module which is used to generating and
 verifying signatures, and as well as loading keys. 
 
The node application holds the ability to communicate with other nodes
to verify and confirm new transactions while maintianing connection with
the client. The node application can provide the following services to the client: transfer, balance and history.

The wallet application makes use of these available nodes to request 
either balance or history. The wallet can also request a transfer 
by providing the receiver key and the amount.

<hr />

The node application comes with 3 predefined nodes which 
can be run by passing the following arguments:
    
    nodename=node1
    port=12111
    
The following can be provided for the given options;
 
    ports: 12111, 12112, 12113
    node names: node1, node2, node3

<hr />

The wallet application also comes with 4 predefined users
which can be run by passing the following arguments:
    
    username=bob 
    nodename=node1 
    command=balance
    
The following can be provided for the given options;

    username: bob, alice, charlie, david
    node names: node1, node2, node3
    commands: balance, transfer, history
    
Each user is also request to provide a password, which is the
name of the user and "1234". example: "bob1234"

<hr />

mvn package will generate jar and fat jar

Command line examples which can be used to execute the jar file.

    java -jar wallet-1.0-SNAPSHOT-jar-with-dependencies.jar username=bob nodename=node1 command=transfer
    java -jar wallet-1.0-SNAPSHOT-jar-with-dependencies.jar username=bob nodename=node1 command=history
    java -jar wallet-1.0-SNAPSHOT-jar-with-dependencies.jar username=bob nodename=node1 command=balance
    
    java -jar node-1.0-SNAPSHOT-jar-with-dependencies.jar nodename=node1 port=12111
    java -jar node-1.0-SNAPSHOT-jar-with-dependencies.jar nodename=node2 port=12112
    java -jar node-1.0-SNAPSHOT-jar-with-dependencies.jar nodename=node3 port=12113


