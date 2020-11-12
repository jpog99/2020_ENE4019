import java.io.*;
import java.util.Scanner;
import java.net.*;
import java.nio.file.*;


class client {
    public static void main(String[] args) throws Exception{
    	int cmdPort = 2020, dataPort = 2121;
    	String ipAddress = "127.0.0.1";
    	
    	if(args.length>0) {
    		if(args.length == 1) {
    			ipAddress = args[0];
    		}
    		else if(args.length == 2) {
    			ipAddress = args[0];
    			cmdPort = Integer.parseInt(args[1]);		
    		}
    		else if(args.length == 3) {
    			ipAddress = args[0];
    			cmdPort = Integer.parseInt(args[1]);
    			dataPort = Integer.parseInt(args[2]);
    		}
    	}
    	
    	Path clientDirectory = Paths.get("");
    	clientDirectory = Paths.get(clientDirectory.toAbsolutePath().toString());
    	
    	System.out.println("Program Running..");
    	System.out.println("Host IP address: " + ipAddress);
		System.out.println("Command port number: " + cmdPort);
		System.out.println("Data port number: " + dataPort  + '\n');
    	
    	boolean appRunning = true;
    	while(appRunning) {
	    	Socket clientSocket = new Socket(ipAddress, cmdPort);
		    String request;
		   	String serverResponse;
		   	String[] responseToken;
	    
	    	Scanner in = new Scanner(System.in);
	    	request = in.nextLine();
	    	String[] requestToken = request.split(" ");
	    	
	    	
	    	DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	    	outToServer.writeBytes(request + "\r\n");
	    	
	    	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

	    	if(requestToken[0].equals("CD")) {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	
		    	if(responseToken[0].equals("200")) {
		    		for(int i=3 ; i<responseToken.length ; i++)
		    			System.out.print(responseToken[i] + " ");
		    		System.out.println('\n');
		    	}
		    	if(responseToken[0].equals("404")) System.out.println("NOT FOUND: Path do not exist.\n");
		    	if(responseToken[0].equals("400")) System.out.println("BAD REQUEST: Path contains illegal character.\n");
		    	
	    	}
	    	
	    	else if(requestToken[0].equals("LIST")) {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	
				
				if(responseToken[0].equals("200")) {
					String fileList = inFromServer.readLine();
					String[] fileListArray = fileList.split("\\|");
					for(int i=0 ; i<fileListArray.length ; i++) 
						System.out.println(fileListArray[i]);
					System.out.println('\n');
				}
				
				if(responseToken[0].equals("400")) System.out.println("BAD REQUEST: Path contains illegal character.\n");
				if(responseToken[0].equals("404")) System.out.println("NOT FOUND: Such directory does not exist.\n");
				
    	    }
    	    else if(requestToken[0].equals("GET")) {
				Socket fileSocket = new Socket(ipAddress , dataPort);
				serverResponse = inFromServer.readLine();
				responseToken = serverResponse.split(" ");
				
		    	if(responseToken[0].equals("200")) {
		    		String filename = inFromServer.readLine();
	    			
	    			System.out.println("Received " + filename + " / " + responseToken[2] + " bytes.");
					InputStream is = fileSocket.getInputStream();
			    	FileOutputStream fout = new FileOutputStream(clientDirectory.toAbsolutePath().toString() + "\\"  + filename);
			    	
			    	byte b[] = new byte[1000];
			    	int chunkLen = 0;
			    	while( (chunkLen = is.read(b)) != -1) {
						fout.write(b,0,chunkLen);
						if(chunkLen>=1000)
							System.out.print("#");
					}
			    	System.out.println(" Completed!\n");
			    	is.close();
			    	
		    	}
		    	
		    	if(responseToken[0].equals("400")) System.out.println("BAD REQUEST: Path contains illegal character.\n");
				if(responseToken[0].equals("404")) System.out.println("NOT FOUND: Such file does not exist or a directory.\n");
		    	fileSocket.close();
		    }
	    	
    	    else if(requestToken[0].equals("PUT")){
	    		Socket fileSocket = new Socket(ipAddress, dataPort);
	    		
	    		//in case filename has spaces
	    		String nameToken[] = new String[requestToken.length-1];
				for(int i=1 ; i<requestToken.length ; i++)
					nameToken[i-1] = requestToken[i];
				String filename = String.join(" ", nameToken);
				
				try {
		    		FileInputStream fs = new FileInputStream(clientDirectory.toAbsolutePath().toString() + "\\"  + filename);
		    		File file = new File(clientDirectory.toAbsolutePath().toString() + "\\"  + filename);
		    		Long filesize = new Long(file.length());
		    		
		    		outToServer.writeBytes(filesize.toString() + "\r\n");
		    		System.out.println(filename + " transfered / " + filesize + " bytes.");
		    		
		    		
			    	serverResponse = inFromServer.readLine();
			    	responseToken = serverResponse.split(" ");
			    	
			    	if(responseToken[0].equals("200")) {
			    		byte b[] = new byte[1000];
			    		int chunkLen = 0;
			    		while ( ( chunkLen = fs.read(b)) != -1 ) {
			    			OutputStream os = fileSocket.getOutputStream();
			    			if(chunkLen >= 1000)
			    				System.out.print("#");
			    			os.write(b, 0, chunkLen);
			    		}
			    		fs.close();	
			    		System.out.println(" Completed!\n");
			    	}
	    		
				}catch(Exception e) {
					System.out.println("NOT FOUND: File does not exist in client directory!");
					outToServer.writeBytes("-1" + "\r\n");
					serverResponse = inFromServer.readLine();
					System.out.println(serverResponse + "\n");
				}
	    		
	    		fileSocket.close();
	    	}
	    	
	    	else if(requestToken[0].equals("QUIT")) {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	
		    	if(responseToken[0].equals("499")) System.out.println("Disconnected from server.  Program terminated.");
		    	
	    		appRunning = false;
	    	}
	    	
	    	else {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	if(responseToken[0].equals("400")) System.out.println("BAD REQUEST: Invalid command.\n");
	    	}
	    	
	    	
	    	clientSocket.close();
    	}
    }
}