# zab_java


Local Dev Environment Setup and Testing.

Step1: install jdk if not present.

	sudo add-apt-repository ppa:openjdk-r/ppa
	sudo apt-get update   
	sudo apt-get install openjdk-7-jdk  

Step2: install maven if not present.

	Download maven 3.3.9 from website and untar
	set JAVA_HOME to your jdk directory
	set PATH to include maven bin directory

Step 3: Clone the git repo 
		
	git clone https://github.com/raviflipsyde/zab_java.git

Step 4: Compile and create executable jar 

	mvn clean install package

Step 5: Test the code on one or more VCLs
	
	1. Starting Bootstrapserver: 
	java -cp zab1-0.0.1.jar Start Bootstrap <bootstrap_port>

	2. Starting a Node: 
	java -cp zab1-0.0.1.jar Start Node <bootstrap_IP> <bootstrap_port> <node_port>

Step 6: Test the Read/Write requests from client

	Client Write Request: 
	echo -e "WRITE:<KEY>:<VALUE>" | nc <node_IP> <node_port>

	Client Read Request: 
	echo -e "READ:<KEY>" | nc <node_IP> <node_port>	

To test this project, start one bootstrap server and once the bootstrap is up and running start one or more node processes (ideally 3 node processes on 3 different VCLs if log analysis is required). 

The leader election and recovery will take around 15 seconds after which the cluster can start processing READ and WRITE requests from the client.

The Commits are stored in CommitedHistory_xxxx.log files in the root directory of the node/ project.
Format of the logs in logfile is [ZxId_Epoch,ZxId_Counter,Key,value]

READ and WRITE requests can be sent to any of the node in the cluster. The correctness of the algorithm can be verified by following 2 parameters
- The CommitedHistory_xxxx.log files on all the nodes will be consistent in the order of the commit.
- The commits in CommitedHistory_xxxx.log files will not contain a smaller ZxId after a bigger ZxId
 



