import java.io.*;
import java.util.*;

import javax.activation.MimetypesFileTypeMap;

import java.net.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class server {
	
	public static void main(String args[]) throws IOException, InterruptedException {						
	
		//Create a Socket for the Server
		ServerSocket serverSocket = new ServerSocket(0);
		System.out.println("Server started successfully.");	
		
		//To keep track of file access count
		HashMap<String, Integer> accessCount = new HashMap<>();
		
		while(true) {
			
			int port = serverSocket.getLocalPort();
			System.out.println(InetAddress.getLocalHost());
			System.out.println("Listening Port: " + port);
		
			//Listen to the Client for the input
			Socket socket = serverSocket.accept();		

			//Get Client IP address and listening port number
			InetAddress clientIP = socket.getInetAddress();		
			
			//Get the input from the Client here
			BufferedReader inputRequest = new BufferedReader(new InputStreamReader(socket.getInputStream()));		
		
			//To send the output to the client from Server
			OutputStream output = new BufferedOutputStream(socket.getOutputStream());
				
			//Initiate thread process
			Threading threadProcess = new Threading(output, inputRequest, accessCount, clientIP, port);
			threadProcess.start();				
		
		}
		
		//Close the server socket
		//serverSocket.close();		
			
	}
	
}

//------------------------------------------------------

// Implementation of Thread
class Threading extends Thread {
	
	OutputStream output;
	BufferedReader thread_input;
	HashMap<String, Integer> accessCount = new HashMap<>();
	InetAddress clientIP;
	int port;
	
	
	String lineSpace = System.getProperty("line.separator");
	byte[] lineSpaceByte = lineSpace.getBytes();
	boolean fileFound = false;
	String fileExtn = null;
	String typeOfFile = null;
	String accessFile = null;
	
	//------------------------------------------------------
	
	@Override
	public void run(){
		
		String fullPath = null;			

		try {
			String fileName[] = thread_input.readLine().toString().split(" ");
			accessFile = fileName[1];
			fullPath = "www" + File.separator + fileName[1];
			}

		catch (IOException ex) {
			System.out.println(Thread.currentThread() + " Exception occured while reading the file name.");
			ex.printStackTrace();
			return;
			}
		
		catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println(Thread.currentThread() + "Exception occured while reading the file extension.");
			ex.printStackTrace();
			return;
		}
		catch (NullPointerException ex) {
			ex.printStackTrace();
		}

		File file = new File(fullPath);
		long fileLen = 0;
		long fileLmd = 0;

		if(file.exists()) {
			fileFound = true;
			fileLen = file.length();
			fileLmd = file.lastModified();
		}
		
		//Construct Message Header
		constructHeader(fileLen, fileLmd);
		
		//If file exists, do construct the body
		if(file.exists()){
			
			try {
				
				//Construct message body												
				FileInputStream responseContentFis = new FileInputStream(fullPath);
																
				BufferedInputStream buffContent = new BufferedInputStream(responseContentFis);
				byte[] content = new byte[8192];						
				
				int contentSize;

				while( (contentSize=buffContent.read(content)) >= 0){
					output.write(content, 0, contentSize);
					}	
				
				output.flush();
				buffContent.close(); 
								
				//--------- Increment the access count for the file ---------
				int count = 1;
				if(accessCount.containsKey(accessFile)) {
					count = accessCount.get(accessFile);
					count++;
					accessCount.replace(accessFile, count);
					}
				else {					
					accessCount.put(accessFile, 1);					
					}

				
				String clientIP1 = clientIP.toString().replace("/", "|");
				System.out.println(accessFile + clientIP1 + "|" + port + "|" + count);
				
				}

			catch(IOException ex){				
				System.out.println(Thread.currentThread() + "Exception occured while writing to response.");
				ex.printStackTrace();
				return;
				}
			
			finally {
				try {
					output.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			}

		else {
			//System.out.println("Cannot locate file: " + accessFile);
			return;
			}
		
		}
	
	//------------------------------------------------------
	
	//Construct message header, send it to the client
	
	public void constructHeader(long fileLen, long fileLmd) {
		
		try {
			
			//Status line
			String status;
			if(fileFound) {
				status = "HTTP/1.1 200 OK";
				}
			else {
				status = "HTTP/1.1 404 Not Found";
				}
			byte[] statusByte = status.getBytes();
			output.write(statusByte, 0, status.length());						
			output.write(lineSpaceByte, 0, lineSpace.length());	
			
		
			//Date
			DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.of("GMT"));
			Date curDate = new Date(); 
			String date = "Date: " + dateFmt.format(curDate.toInstant());
			byte[] dateByte = date.getBytes();
			output.write(dateByte, 0, date.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
		
			//Server Name
			String debian = "Server: Apache/2.2.16 (Debian)";
			byte[] debianByte = debian.getBytes();
			output.write(debianByte, 0, debian.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
		
			//Last modified Date
			Date lmdDate = new Date(fileLmd);
			String lmd;
			if(fileFound) {
				lmd = "Last-Modified: " + dateFmt.format(lmdDate.toInstant());
			}else {
				lmd = "Last-Modified: " ;
			}								
			
			byte[] lmdByte = lmd.getBytes();
			output.write(lmdByte, 0, lmd.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
		
			//Accept in Bytes
			String accept = "Accept-Ranges: bytes";
			byte[] acceptByte = accept.getBytes();
			output.write(acceptByte, 0, accept.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
		
			//File length
			long len = fileLen;
			String fileLength = "Content-Length: " + Long.toString(len);
			byte[] fileLengthByte = fileLength.getBytes();
			output.write(fileLengthByte, 0, fileLength.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
		
		
			//Type of file
			MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
			String mimeType = mimeTypesMap.getContentType(accessFile);
	/*		
			String newtemp[] = accessFile.split("\\.");			
			fileExtn = newtemp[1];
			if(fileExtn.equals("html")) {
				typeOfFile = "Content-Type: text/html";
				}
			else if (fileExtn.equals("pdf")) {
				typeOfFile = "Content-Type: application/pdf";
				}
			else if (fileExtn.equals("tif")) {
				typeOfFile = "Content-Type: image/tiff";
				}
			else if (fileExtn.equals("deb")) {
				typeOfFile = "Content-Type: application/x-debian-package";
			}
			else {
				typeOfFile = "Content-Type: application/octet-stream";
			}
			*/

			typeOfFile ="Content-Type: " +  mimeType;
			
			if(!fileFound) {
				typeOfFile = "Content-Type: text/html";
			}
				
			byte[] typeOfFileByte = typeOfFile.getBytes();
			output.write(typeOfFileByte, 0, typeOfFile.length());

		
			//Insert a line space for separation
			output.write(lineSpaceByte, 0, lineSpace.length());
			output.write(lineSpaceByte, 0, lineSpace.length());
			
			if(!fileFound) {
				output.flush();
				output.close();
			}
			
		
		}
	
		catch (IOException ex) {
			System.out.println("SERVER: Issue while writing header.");
			ex.printStackTrace();
			return;
		}
				
	}
			
	//------------------------------------------------------
	
	//Constructor to receive the inputs
	Threading(OutputStream output, BufferedReader thread_input, HashMap<String, Integer> accessCount, InetAddress clientIP, int port) {
		this.output = output;
		this.thread_input = thread_input;
		this.accessCount = accessCount;
		this.clientIP = clientIP;
		this.port = port;
	}
		
}


