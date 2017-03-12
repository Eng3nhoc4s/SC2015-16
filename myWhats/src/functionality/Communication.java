package functionality;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class handles communication between client/server
 */
public class Communication {

	/**
	 * Connects to a server in the address and port provided
	 * 
	 * @param addressAndPort
	 *            - Server's address and port in X.X.X.X:YYYY FORMAT
	 * @return A socket with the connection to the server, null in case of fail
	 */
	public static Socket connect(String addressAndPort) {

		Socket sock = null;

		try {
			if (addressAndPort.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\:\\d+")) {
				String[] addPort = addressAndPort.split(":");
				SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
				sock = ssf.createSocket(addPort[0], Integer.parseInt(addPort[1]));
							
				SSLSession session = ((SSLSocket) sock).getSession();

			    System.out.println("The Certificates used by peer");

			    System.out.println("Peer host is " + session.getPeerHost());
			    System.out.println("Cipher is " + session.getCipherSuite());
			    System.out.println("Protocol is " + session.getProtocol());
			    System.out.println("Session created in " + session.getCreationTime());
			    System.out.println("Session accessed in " + session.getLastAccessedTime());
			}
		} catch (IOException e) {

			System.err.println("Error connecting to server!\nCheck if server or connection are down!");
			// e.printStackTrace();
		}
		return sock;
	}

	/**
	 * Sends a command flag and it's arguments to the server
	 * 
	 * @param out
	 *            - ObjectOutputStream
	 * @param flag
	 *            - The command flag for the operation
	 * @param args
	 *            - The arguments for the operation
	 */
	public static void sendCommand(ObjectOutputStream out, String flag, String[] args) {

		try {
			out.writeObject(flag);
			out.writeObject(args);
			out.flush();

		} catch (IOException e) {
			System.err.println("Error sending flag and arguments to server!");
			// e.printStackTrace();
		}
	}
	
	public static void sendFileOrGroup (ObjectOutputStream out, String dest, String serverpass){
		
		String [] userList = null;
		
		try {
			//Enviar se é cliente ou grupo
			//0 - Group
			//1 - User
			int val = Group.isUserOrGroup(dest, serverpass);
			
			//Se der erro
			if(val == -1){
				out.writeInt(-1);
				out.flush();
			
			//É Grupo
			}else if(val == 0){
				
				out.writeInt(0);
				out.flush();
				userList = Group.membersList(Configurations.GROUPS_FOLDER + "/" + dest + ".cfg");
				out.writeObject(userList);
				out.flush();
			
			//É user
			}else if(val == 1){
				
				userList = new String [] {dest};
				out.writeInt(1);
				out.flush();
				out.writeObject(userList);
				out.flush();
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String [] receiveUserOrGroup(ObjectInputStream in){
		
		String [] memberList = null;
		
		try {
			//receive group/user/error indication

			int val = in.readInt();
			
			//Error
			if(val == -1){
				System.err.println("Error receiving if user or group!");
				
			//GROUP
			}else if(val == 0){
				
				memberList = (String []) in.readObject();
			
			//USER
			}else if(val == 1){
				memberList = (String []) in.readObject();
			}
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return memberList;
	}
}
