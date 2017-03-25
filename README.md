# zab_java


Local Dev Environment Setup.

Step1: install jdk if not present.

	sudo add-apt-repository ppa:openjdk-r/ppa
	sudo apt-get update   
	sudo apt-get install openjdk-7-jdk  

Step2: install maven if not present.

	Download maven 3.3.9 from website and untar
	set JAVA_HOME to your jdk directory
	set PATH to include maven bin directory

Step 3: git clone https://github.com/raviflipsyde/zab_java.git

Step 4: mvn clean install package

Starting Bootstrapserver: java -cp zab1-0.0.1.jar Start Server <bootstrap_port>

Starting Node: java -cp zab1-0.0.1.jar Start Node <bootstrap_IP> <bootstrap_port> <node_port>


