import java.io.*;

import java.util.Scanner;
import java.net.*;
import java.nio.*;
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
		
		//target for DROP,BITERR,TIMEOUT
		int target = -1;
		String errorType = "";
    	
    	boolean appRunning = true;
    	while(appRunning) {
    		try {
	    	Socket clientSocket = new Socket(ipAddress, cmdPort);
		    String request;
		   	String serverResponse;
		   	String[] responseToken;
		   	
	        System.out.print("\n>");
	    	Scanner in = new Scanner(System.in);
	    	request = in.nextLine();
	    	String[] requestToken = request.split(" ");
	    	
	    	//variables for DROP,TIMEOUT,BITERROR command
	    	boolean lastFrameSent = false;
	    	int windowSize = 5;
	    	int sent = 0;
	    	int corruptedPacket = -1;
	    	boolean corruptExist = false;	    	
	    	String retransmitMessage = "";
	    	String acks = "";
	    	
	    	
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
	    			DataInputStream is = new DataInputStream(fileSocket.getInputStream());
			    	FileOutputStream fout = new FileOutputStream(clientDirectory.toAbsolutePath().toString() + "\\"  + filename);
			    	
			    	byte chunk[] = new byte[1003];
			    	int chunkLen = 0;

			    	while(true) {
		    			chunkLen = is.read(chunk);
		    			if(chunkLen == -1) break;
		    			
		    			//get the seqNo from server for each chunk
		    			ByteBuffer bb = ByteBuffer.allocate(1);
		    			bb.order(ByteOrder.LITTLE_ENDIAN);
		    			bb.put(chunk[0]);
		    			short seqNo = bb.get(0);
		    			
		    			//write the file, ignoring first 3 bytes (seqNo+chkSum)
		    			fout.write(chunk,3,chunkLen-3);
		    			
		    			if(chunkLen>=1000)
		    				System.out.print(seqNo + " ");
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
		    		
		    		File file = new File(clientDirectory.toAbsolutePath().toString() + "\\"  + filename);
		    		FileInputStream fs = new FileInputStream(file);
		    		Long filesize = new Long(file.length());
		    		
		    		outToServer.writeBytes(filesize.toString() + "\r\n");
		    		System.out.println(filename + " transfered / " + filesize + " bytes.");
		    		
		    		
			    	serverResponse = inFromServer.readLine();
			    	responseToken = serverResponse.split(" ");
			    	
			    	if(responseToken[0].equals("200")) {
			    		DataOutputStream chunkToServer = new DataOutputStream(fileSocket.getOutputStream());
			    		
			    		//calculate how many chunks to be sent
			    		int totalPackets = (int)(filesize/1000);
			    		if(filesize%1000 != 0) totalPackets++;
			    		
			    		byte data[] = new byte[1000];
			    		byte corrupted[] = null; //byte array to store corrupted chunk for transmission
			    		int dataLen = 0;
			    		byte seqNo = 0;
			    		while (!lastFrameSent) {
			    			Thread.sleep(100); //make thread sleep a bit everytime a window is sent to receive acks.
			    			outToServer.writeBytes("Window start\n");
			    			for(int i=0 ; i<windowSize ; i++) {
				    			ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
				    			ByteArrayOutputStream corrStream = new ByteArrayOutputStream();
				    			
				    			//retransmit corrupted chunk if any
				    			if(corruptExist) {
				    				outToServer.writeBytes("In window\n");
				    				chunkToServer.write(corrupted, 0, corrupted.length);
				    				sent++;
				    				target=-1;
				    				corruptExist = false;
				    				break;
				    			}
				    			else {
				    				dataLen = fs.read(data);
				    				if(dataLen == -1 ) {
				    					lastFrameSent = true;
				    					break;
				    				}
				    			}
				    			//convert short chkSum to byte array
				    			short chkSum = 0x0000;
				    			byte[] c = new byte[2];
				    			c[0] = (byte)(chkSum & 0xff);
				    			c[1] = (byte)((chkSum >> 8) & 0xff);
				    			
				    			//convert short size to byte array
				    			short size = (short)(dataLen + 5);
				    			byte[] s = new byte[2];
				    			s[0] = (byte)(size & 0xff);
				    			s[1] = (byte)((size >> 8) & 0xff);
				    			
				    			byteOS.write(seqNo);
				    			byteOS.write(c);
				    			byteOS.write(s);
				    			byteOS.write(data);
				    			
				    			//combine all byte arrays to a single byte array as "chunk"
				    			byte[] chunk = byteOS.toByteArray();

				    			//if there's packet to be dropped
			    				if(corruptedPacket!=-1 || seqNo==target) {
			    					
			    					//sleep 1 second before send
			    					if(errorType.equals("TIMEOUT")) 
			    						Thread.sleep(1000);
			    					
			    					//change chkSum of the chunk to 0xffff before send
			    					if(errorType.equals("BITERROR")) {
			    						chkSum = (short) 0xffff;
						    			chunk[1] = (byte)(chkSum & 0xff);
						    			chunk[2] = (byte)((chkSum >> 8) & 0xff);
			    					}
			    					
			    					chunkToServer.write(chunk, 0, dataLen+5);
			    					System.out.print(seqNo + " ");
			    					
			    					//save the dropped chunk into corrupted[] to be transmitted later
			    					corrStream.write(chunk);
			    					corrupted = corrStream.toByteArray();
			    					
			    					outToServer.writeBytes("In window\n");
			    					corruptedPacket=-1;
			    					if(corruptedPacket==totalPackets)
						               	break;
						               if(lastFrameSent)
						               	break;
			    				}
			    				
			    				//if the chunk is not the corrupt target
			    				else {
			    					if(sent==totalPackets){
			    						lastFrameSent=true;
			    						break;
			  					  	}
			    					System.out.print(seqNo + " ");
			    					outToServer.writeBytes("In window\n");
			    					chunkToServer.write(chunk, 0, dataLen+5);
			    					acks += seqNo + " acked, ";
			    					sent++;
			    				}
			    				
			    				seqNo++;
			    			}
			    			//after finished send all chunk in window 
			    			outToServer.writeBytes("Window end\n");
			    				
			    			//read from server if there's any corrupted chunk
			    			corruptedPacket=Integer.parseInt(inFromServer.readLine());
			    			if(corruptedPacket!=-1) {
			    				corruptExist = true;
			    				lastFrameSent = false;
			    				retransmitMessage += "Packet " + corruptedPacket + " did not acked and retransmitted.";
			    			}
			    				
			    			
			    		}
			    		fs.close();	
			    		outToServer.writeBytes("Stop\n");
			    		System.out.println(" Completed!");
			    		System.out.println(acks);
			    		if(!retransmitMessage.equals(""))
							System.out.println(retransmitMessage+"\n");
			    	}
	    		
				}catch(FileNotFoundException e) {
					System.out.println("NOT FOUND: File does not exist in client directory!");
					outToServer.writeBytes("-1" + "\r\n");
					serverResponse = inFromServer.readLine();
					System.out.println(serverResponse + "\n");
				}
				
	    		errorType = "";
	    		fileSocket.close();
	    	}
	    	
	    	else if(requestToken[0].equals("QUIT")) {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	
		    	if(responseToken[0].equals("499")) System.out.println("Disconnected from server.  Program terminated.");
		    	
	    		appRunning = false;
	    	}
	    	
	    	else if(requestToken[0].equals("DROP") || requestToken[0].equals("TIMEOUT") || requestToken[0].equals("BITERROR")) {
	    		//no argument passed
	    		if(requestToken.length<2 || !requestToken[1].matches(".*\\d.*")) {
	    			System.out.println("BAD REQUEST: No argument passed or invalid argument.");
	    			outToServer.writeBytes("-1\n");		
	    		}
	    		//too many argument
	    		else if( requestToken[1].contains(",") || requestToken[1].contains(" ")) {
	    			System.out.println("BAD REQUEST: Can only target 1 packet.");
	    			outToServer.writeBytes("-1\n");
	    			
	    		}
	    		else {
	    			errorType = requestToken[0];
	    			String intValue = requestToken[1].replaceAll("[^0-9]", "");
	    			target = Integer.parseInt(intValue);
	    			outToServer.writeBytes(intValue+"\r\n");
	    		}
	    		
	    	}
	    	
	    	else {
		    	serverResponse = inFromServer.readLine();
		    	responseToken = serverResponse.split(" ");
		    	if(responseToken[0].equals("400")) System.out.println("BAD REQUEST: Invalid command.\n");
	    	}
	    	
	    	
	    	clientSocket.close();
    	}catch(ConnectException e) {
    		System.out.println("Server is not online.");
    		System.exit(0);
    	}
    		
    	}
    }
}