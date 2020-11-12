
import java.io.*;
import java.net.*;
import java.nio.file.*;


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
    	
    	while(true) {
	    	Socket connectionSocket = welcomeSocket.accept();
	    	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	    	DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	    	clientRequest = inFromClient.readLine();
	    	System.out.println("Request: "  + clientRequest);
	    	int flag = 0;
	   		
	    	String[] token = clientRequest.split(" ");
	    	String command = token[0];
	    	
	    	String pathToken[] = new String[token.length-1];
			for(int i=1 ; i<token.length ; i++)
				pathToken[i-1] = token[i];
			String pathname = String.join(" ", pathToken);
	    	
	    	
	    	switch(command) {
	 
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
					
					Long fsize = new Long(tempPath.toFile().length());
					byte[] b = new byte[1000];
					int chunkLen = 0;
					while( (chunkLen = fs.read(b)) != -1) {
						OutputStream os = file_s.getOutputStream();
						os.write(b,0,chunkLen);
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
	    			
	    			responseMessage = "200 Ready to receive";
					outToClient.writeBytes(responseMessage + "\r\n");
					System.out.println("Response: " + responseMessage + "\n");
		    		
					InputStream is = file_s.getInputStream();
		    		FileOutputStream fout = new FileOutputStream(currentPath.toAbsolutePath().toString() + "\\" + filename );
		    		
		    		byte b[] = new byte[1000];
		    		int chunkLen = 0;
		    		while( (chunkLen = is.read(b) ) != -1) {
		    			fout.write(b,0,chunkLen);
		    		}
 		
		    		file_s.close();
					file_socket.close();
    			}
				
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
    	}
    	
    }
}