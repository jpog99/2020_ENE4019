
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.ArrayList;


class server {
    public static void main(String[] args) throws Exception{
    	int cmdPort = 2020, dataPort = 2121;
    	
    	if(args.length>0) {
    		if(args.length == 1) {
    			cmdPort = Integer.parseInt(args[0]);
    		}
    		else if(args.length == 2) {
    			cmdPort = Integer.parseInt(args[0]);
    			dataPort = Integer.parseInt(args[1]);
    		}
    	}
    	
    		
    	
    	String clientRequest, serverResponse = "";
    	String responseMessage = "";
    	String filename = null;
    	Path currentPath = Paths.get("");
    	Path tempPath = null;
    	currentPath = Paths.get(currentPath.toAbsolutePath().toString());
    	
    	ServerSocket welcomeSocket = new ServerSocket(cmdPort);
    	ServerSocket file_socket;
		Socket file_s;
		
		System.out.println("Server is ready!");
		System.out.println("Command port number: " + cmdPort);
		System.out.println("Data port number: " + dataPort + '\n');
		
		//packet number for DROP,BITERR,TIMEOUT
		int corr=-1;
		String errorType = "";
    	
    	while(true) {
    		try {
	    	Socket connectionSocket = welcomeSocket.accept();
	    	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	    	DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	    	clientRequest = inFromClient.readLine();
	    	System.out.println("Request: "  + clientRequest);
	    	int flag = 0;
	    	
	    	//variables for DROP,TIMEOUT,BITERROR command
			ArrayList<byte[]> currentWindow = new ArrayList<byte[]>();
			ArrayList<byte[]> buffer = new ArrayList<byte[]>();
	    	int window=5;
	    	boolean corruptExist = false;
	   		
	    	String[] token = clientRequest.split(" ");
	    	String command = token[0];
	    	
	    	String pathToken[] = new String[token.length-1];
			for(int i=1 ; i<token.length ; i++)
				pathToken[i-1] = token[i];
			String pathname = String.join(" ", pathToken);
	    	
	    	
	    	switch(command) {
	    	
	/*==============================COMMAND DROP,TIMEOUT,BITERROR <packet num>===================================*/  	
	    	
	    	case "DROP":
	    		corr = Integer.parseInt(inFromClient.readLine());
	    		if(corr!=-1) {
	    			System.out.println("Response: Packet number " + corr + " will be dropped.\n");
	    			errorType = command;
	    		}
	    		else
	    			System.out.println("400 BAD REQUEST: No argument or too many arguments passed.\n");
	    		break;
	    	
	    	case "TIMEOUT":
	    		corr = Integer.parseInt(inFromClient.readLine());
	    		if(corr!=-1) {
	    			System.out.println("Response: Packet number " + corr + " will be timed out.\n");
	    			errorType = command;
	    		}
	    		else
	    			System.out.println("400 BAD REQUEST: No argument or too many arguments passed.\n");
	    		break;
	    		
	    	case "BITERROR":
	    		corr = Integer.parseInt(inFromClient.readLine());
	    		if(corr!=-1) {
	    			System.out.println("Response: Packet number " + corr + " will be corrupted.\n");
	    			errorType = command;
	    		}
	    		else
	    			System.out.println("400 BAD REQUEST: No argument or too many arguments passed.\n");
	    		break;
	 
	/*==============================COMMAND CD <PATHNAME>===================================*/
	    	case "CD" :
	    		if(token.length == 1) 
	    			responseMessage = "200 Moved to " +  currentPath.toAbsolutePath().toString();
	    		
	    		if(token.length > 1) {
	    			//if CD ., return current dir
	    			if(token[1].equals(".")) {
	    				responseMessage = "200 Moved to " +  currentPath.toAbsolutePath().toString();
	    	    		//serverResponse = currentPath.toAbsolutePath().toString();
	    			}
	    			//if CD .., return parent dir
	    			else if(token[1].equals("..")) {
	    				currentPath = Paths.get(currentPath.toAbsolutePath().getParent().toString());
	    				responseMessage = "200 Moved to " +  currentPath.toAbsolutePath().toString();
	    			}
	    			
	    			//if CD <pathname>
	    			else {
						try{
							tempPath = Paths.get(pathname + "\\");
							//if pathname is absolute
							if(tempPath.isAbsolute()) {
								if(tempPath.toFile().isDirectory() && tempPath.toFile().exists()) {
									currentPath = tempPath;
									responseMessage = "200 Moved to " +  currentPath.toAbsolutePath().toString();
								}
								else {
									responseMessage = "404 NOT FOUND";
								}			
							}
							
							//if pathname is relative
							else {
								String nameToken[] = new String[token.length-1];
								for(int i=1 ; i<token.length ; i++)
									nameToken[i-1] = token[i];
								filename = String.join(" ", nameToken);
								
								tempPath = Paths.get(currentPath.toAbsolutePath().toString() + "\\" + filename);
								//if pathname exist and a directory
								if(tempPath.toFile().isDirectory() && tempPath.toFile().exists()) {
									currentPath = tempPath;
									responseMessage = "200 Moved to " +  currentPath.toAbsolutePath().toString();
								}
								else {
									responseMessage = "404 NOT FOUND";
								}
							}
						}catch(Exception e) {
							responseMessage = "400 BAD REQUEST";
						}
					}
	    		}
	    		
	    		System.out.println("Response: " + responseMessage + "\n");
	    		outToClient.writeBytes(responseMessage + "\r\n");
	    		break; 
	    		
	    		
	/*============================COMMAND LIST <PATHNAME>==================================*/
	    	case "LIST":
	    		String[] list,filesize; 
	    		String listString = "";
	    		if(token.length == 1) { tempPath = currentPath; flag = 1;}
	    		else if(token[1].equals(".")) { tempPath = currentPath; flag = 1;}
	    		else if(token[1].equals("..")){
	    			tempPath = Paths.get(currentPath.toAbsolutePath().getParent().toString());
	    			flag = 1;
	    		}
	    		
	    		//LIST <pathname>
	    		else {
	    			try {
		    			tempPath = Paths.get(pathname + "\\");
		    			//absolute pathname
		    			if(tempPath.isAbsolute()) {
		    				if(tempPath.toFile().isDirectory() && tempPath.toFile().exists()) {
		    					tempPath = tempPath;
		    					flag = 1;
		    				}		    					
		    				else {
		    					responseMessage = "404 NOT FOUND";
		    				}
		    			}
		    				
		    			//relative pathname
		    			else {
		    				String nameToken[] = new String[token.length-1];
			    			for(int i=1 ; i<token.length ; i++)
			    				nameToken[i-1] = token[i];
			    			filename = String.join(" ", nameToken);
			    			
				   			tempPath = Paths.get(currentPath.toAbsolutePath().toString() + "\\" + filename);
				   			
			    			if(!tempPath.toFile().isDirectory() || !tempPath.toFile().exists()) {
		    					responseMessage = "404 NOT FOUND";
			    			}else 
			    				flag = 1;
		    			}
	    			}catch(Exception e) {
						responseMessage = "400 BAD REQUEST";
	    			}
	    		}
	    		
	    		if(flag == 1) {
		    		list = tempPath.toFile().list();
		    		filesize = new String[list.length];
		    		responseMessage = "200 Comprising " +list.length+" entries."; 
		    		outToClient.writeBytes(responseMessage + "\r\n");
		    		
		    		for(int i=0 ; i<list.length ; i++) {
		    			//get all filenames and make it File obj
		    			File f = new File(tempPath.toString() + "\\" + list[i]);
		    			if(f.isDirectory()) filesize[i] = "-";
		    			else { Long s = new Long(f.length()); filesize[i] = s.toString();}
		    			listString += list[i] + "," + filesize[i] + "|";
		    		}
		    		
		    		outToClient.writeBytes(listString + "\r\n");
	    		}else {	
	    			outToClient.writeBytes(responseMessage + "\n");
	    		}
	    		
	    		System.out.println("Response: " + responseMessage + "\n");
	    		break;
	    	
	    		
      /*=============================COMMAND GET <FILENAME>====================================*/
	    	case "GET":
	    		
	    		file_socket = new ServerSocket(dataPort);
	    		file_s = file_socket.accept();
	    		
	    		if(token.length == 1) {
					responseMessage = "404 NOT FOUND";
					file_s.close();
					file_socket.close();
	    		}
	    		
	    		try {
			    	tempPath = Paths.get(pathname + "\\");
			    	//path is absolute
		    		if(tempPath.isAbsolute()) {
		    			if(!tempPath.toFile().isDirectory() && tempPath.toFile().exists()) {
		    				tempPath = tempPath;
		    				filename = tempPath.toFile().getName();
		    				flag = 1;
		    			}
		    			else {		    				
	    					responseMessage = "404 NOT FOUND";
							file_s.close();
							file_socket.close();
		    			}
		    		}
		    				
		    		//path is relative
		    		else {
		    			String nameToken[] = new String[token.length-1];
		    			for(int i=1 ; i<token.length ; i++)
		    				nameToken[i-1] = token[i];
		    			filename = String.join(" ", nameToken);
		    			
			   			tempPath = Paths.get(currentPath.toAbsolutePath().toString() + "\\" + filename);
			   			
		    			if(!tempPath.toFile().exists()) {
	    					responseMessage = "404 NOT FOUND";
							file_s.close();
							file_socket.close();
		   				}
		    			
		    			else if(tempPath.toFile().isDirectory()) {
	    					responseMessage = "404 NOT FOUND";
							file_s.close();
							file_socket.close();
		    			}
		   			}		
	   			}catch(Exception e) {
					responseMessage = "400 BAD REQUEST";
					file_s.close();
					file_socket.close();
	   			}
	    		

	    			
				try {	
					FileInputStream fs = new FileInputStream(tempPath.toString());
					DataOutputStream chunkToClient = new DataOutputStream(file_s.getOutputStream());
					
					Long fsize = new Long(tempPath.toFile().length());
					byte[] data = new byte[1000];
					int dataLen = 0;
					byte seqNo = 0;
					
					while(true) {
						ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		    			dataLen = fs.read(data);
		    			if(dataLen == -1 ) break;
		    			
		    			//convert short chkSum to byte array
		    			short chkSum = 0x0000;
		    			byte[] c = new byte[2];
		    			c[0] = (byte)(chkSum & 0xff);
		    			c[1] = (byte)((chkSum >> 8) & 0xff);
		    			
		    			
		    			byteOS.write(seqNo);
		    			byteOS.write(c);
		    			byteOS.write(data);
		    			
		    			//combine all byte arrays to a single byte array as "chunk"
		    			byte[] chunk = byteOS.toByteArray();
		    			
		    			//send the chunk with seqNo (1 byte) + chkSum(2 bytes)
		    			chunkToClient.write(chunk, 0, dataLen + 3);
		    			seqNo++;
					}
					fs.close();
					
						
					responseMessage = "200 Containing " + fsize + " bytes in total.";
					outToClient.writeBytes(responseMessage + "\r\n");	
					outToClient.writeBytes(tempPath.toFile().getName() + "\r\n");
					file_s.close();
					file_socket.close();			
		   		}catch(Exception e){
					responseMessage = "404 NOT FOUND";
					file_s.close();
					file_socket.close();
					outToClient.writeBytes(responseMessage + "\r\n");	
		    		}
    		
	    		System.out.println("Response: " + responseMessage + "\n");
    			
				break;
				
			
        /*=============================COMMAND PUT <FILENAME>====================================*/
	    	case "PUT":
	    		file_socket = new ServerSocket(dataPort);
	    		file_s = file_socket.accept();
	    		String fsize = inFromClient.readLine();
	    		

	    		//in case filename has spaces
	    		String nameToken[] = new String[token.length-1];
    			for(int i=1 ; i<token.length ; i++)
    				nameToken[i-1] = token[i];
    			filename = String.join(" ", nameToken);
    			
    			if(fsize.equals("-1")) {
    				responseMessage = "500 INTERNAL SERVER ERROR";
    				System.out.println("Response: " + responseMessage + "\n");
    				outToClient.writeBytes(responseMessage + "\r\n");
    				file_s.close();
    				file_socket.close();
    			}
	    		
    			else {
	    			System.out.println("Request: "  + fsize);
	    			int size = Integer.parseInt(fsize);
	    			
	    			//calculate how many packets to be received
	    			int totalPackets = size/1000;
		    		if(size%1000 != 0) totalPackets++;
	    			
	    			responseMessage = "200 Ready to receive";
					outToClient.writeBytes(responseMessage + "\r\n");
					System.out.println("Response: " + responseMessage + "\n");
		    		
					DataInputStream is = new DataInputStream(file_s.getInputStream());
		    		FileOutputStream fout = new FileOutputStream(currentPath.toAbsolutePath().toString() + "\\" + filename );
		    		ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		    		String line;
		    		while(true) {
		    			
		    			line = inFromClient.readLine();
		    			if(line.equals("Stop"))
						{
						  System.out.println("All packet received!\n");
						  break;
						}
		    			else if(line.equals("Window start"))
						{
		    				currentWindow.clear();
				    		
				    		int chunkLen = 0;
				    		String status = inFromClient.readLine();
				    		while(!status.equals("Window end")) {
				    			//all the incoming chunks are stored in ArrayList window
				    			byte chunk[] = new byte[1005];
					    		chunkLen = is.read(chunk);
					    		currentWindow.add(chunk);		
					    		status = inFromClient.readLine();    			
				    		}
				    		
				    		//loop to check if any chunk in current window are corrupted
				    		for(int i=0 ; i<currentWindow.size() ; i++) {
				    			byte[] sample = currentWindow.get(i);;
				    			byte seqNo = sample[0];
				    			
				    			//extract chkSum from packet
			    				ByteBuffer bb = ByteBuffer.allocate(2);
			    				bb.order(ByteOrder.LITTLE_ENDIAN);
			    				bb.put(sample[1]);
			    				bb.put(sample[2]);
			    				short chkSum = bb.getShort(0);
				    			
			    				//if current packet is clean, write to file
				    			if(seqNo!=corr && !corruptExist){
				    				System.out.println("packet " + seqNo + " received." );
				    				fout.write(sample, 5, sample.length-5);	
				    				
				    				//if buffer isnt empty, write chunks in buffer to file
				    				while(buffer.size()>0) {
				
				    					byte[] bufToFile = buffer.get(0);
				    					fout.write(bufToFile, 5, bufToFile.length-5);
				    					buffer.remove(0);
				    				}
				    				
				    			//if corrupt exist, store next chunks in buffer
				    			}else if(corruptExist) {
				    				System.out.println("packet " + seqNo + " stored in buffer.");
				    				buffer.add(sample);
				    			
				    			//if seqNo == corrupted packet number
				    			}else {
				    				corruptExist = true;

				    				if(errorType.equals("DROP")) System.out.println("packet " + seqNo + " is dropped. Request for retransmission..");
				    				if(errorType.equals("TIMEOUT")) System.out.println("packet " + seqNo + " timed out. Request for retransmission..");
				    				if(errorType.equals("BITERROR")) System.out.println("packet " + seqNo + " is corrupted.. (Chksum: " + chkSum + ") . Request for retransmission..");
				    			}
				    			
				    		}
				    		
				    		//tell client which chunk is corrupted
				    		if(corruptExist) {
						    	outToClient.writeBytes(String.valueOf(corr) + "\r\n");
						    	corr=-1;
						    	corruptExist = false;
							}else {
						 		outToClient.writeBytes(String.valueOf("-1") + "\r\n");
						   	}
				    		
				    		
						}
		    		}
		    		fout.close();
		    		is.close();
    			}
    			
		
	    		file_s.close();
				file_socket.close();
				errorType = "";
				corr = -1;
				
	    		break;
	    		
	   /*=============================COMMAND QUIT====================================*/	    		
	    	case "QUIT":
	    		responseMessage = "499 Client Closed Request";
	    		outToClient.writeBytes(responseMessage + "\r\n");
	    		System.out.println("Response: " + responseMessage + "\n");
	    		break;
	    		
	    	default:
				responseMessage = "400 BAD REQUEST";
				outToClient.writeBytes(responseMessage + "\r\n");
				System.out.println("Response: " + responseMessage + "\n");
	    		break;
	    	}
	    	

	   		connectionSocket.close();
    	}catch(SocketException e) {
    		System.out.println("Client disconnected unexpectedly. Terminating program.");
    		System.exit(0);
    	}
    	}
    	
    }
}
