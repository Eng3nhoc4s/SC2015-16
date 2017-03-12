package server;

/***************************************************************************
 *					Seguranca e Confiabilidade 2015/16
 ***************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import functionality.*;

//Servidor do servico myWhatsServer
public class myWhatsServer {

	private static String serverpass;

	// MAIN
	public static void main(String[] args) {

		System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "storepass");
		myWhatsServer sv = new myWhatsServer();
		sv.startServer();
	}

	// START SERVER
	@SuppressWarnings("resource")
	public void startServer() {

		ServerSocket sSoc = null;

		serverpass = "";
		boolean validPassLength = false;
		Scanner sc = new Scanner(System.in);

		// ASK FOR SERVER PASSWORD
		System.out.println("Server starting...");
		System.out.print("Password: ");
		while (!validPassLength) {

			serverpass = sc.nextLine();

			if (serverpass.length() < 4) {
				System.err.println("Password must have at least 4 chars.");
			} else {
				validPassLength = true;
			}
		}

		try {

			SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			sSoc = ssf.createServerSocket(23456);

		} catch (IOException e) {

			System.err.println(e.getMessage());
			System.exit(-1);

		}

		// *****************************************************************************
		// CHECK FOR FILE STRUCTURE INTEGRITY
		// *****************************************************************************

		// Check if USERS file exist
		File credentials = new File(Configurations.CREDENTIALS_FILENAME);
		if (!credentials.exists() && !credentials.isDirectory()) {

			try {

				credentials.createNewFile();

			} catch (IOException e) {
				System.err.println("Error creating user file");
				e.printStackTrace();

			}
		}

		// Check if GROUPS file exist
		File groups = new File(Configurations.GROUPS_FILENAME);
		if (!groups.exists() && !groups.isDirectory()) {

			try {

				groups.createNewFile();

			} catch (IOException e) {
				System.err.println("Error creating groups file");
				e.printStackTrace();

			}
		}

		// Check if users FOLDER exist
		File usersFolder = new File(Configurations.USERS_FOLDER);
		if (!usersFolder.exists()) {

			usersFolder.mkdir();
		}

		// Check if groups FOLDER exist
		File groupsFolder = new File(Configurations.GROUPS_FOLDER);
		if (!groupsFolder.exists()) {

			groupsFolder.mkdir();
		}

		// Check if messages FOLDER exist
		File messagesFolder = new File(Configurations.MESSAGES_FOLDER);
		if (!messagesFolder.exists()) {

			messagesFolder.mkdir();
		}

		// *****************************************************************************
		// CHECK FOR MAC FILE
		// *****************************************************************************

		// CALCULATE MAC
		byte[] credentialsMacDigest = Crypto.calculateFileMAC(serverpass, Configurations.CREDENTIALS_FILENAME);

		// CHECK IF PASS FILE IS PROTECTED WITH MAC
		if (Crypto.isMacProtected(Configurations.CREDENTIALS_FILENAME)) {

			// IF SO OK
			System.out.println("Checking credentials file for integrity...");
			if (Crypto.hasValidMac(Configurations.CREDENTIALS_FILENAME, serverpass)) {
				System.out.println("OK");

				// IF NOT ERROR AND CLOSE
			} else {
				System.err.println("The credentials' file calculated mac doesn't match the stored mac!");
				System.exit(-1);
			}

			// IF NOT PROTECT
		} else {
			System.out.println("The credentials file is not protected by MAC, do you wish to protect it (Y/N)?");

			Scanner read = new Scanner(System.in);
			String ans = "";

			while (!ans.equals("n") && !ans.equals("y")) {
				ans = read.nextLine().toLowerCase();
			}

			if (ans.equals("y")) {

				Crypto.writeMacFile(credentialsMacDigest, Configurations.CREDENTIALS_FILENAME);
				// System.out.println("Server started!");

			} else {
				System.err.println("Terminating execution.");
				System.exit(-1);
			}
		}

		// *****************************************************************************
		// CHECK OTHER CONFIG FILES FOR MAC FILE
		// *****************************************************************************

		// GROUPS FILE //
		// Se estiver, verifica se é vaído
		if (Crypto.isMacProtected(Configurations.GROUPS_FILENAME)) {
			System.out.println("Checking groups config file for integrity...");
			Crypto.hasValidMac(Configurations.GROUPS_FILENAME, serverpass);
			System.out.println("OK");
			// Caso contrario protege
		} else {
			byte[] groupsFile = Crypto.calculateFileMAC(serverpass, Configurations.GROUPS_FILENAME);
			Crypto.writeMacFile(groupsFile, Configurations.GROUPS_FILENAME);
		}

		// SERVER FILE //
		// Se estiver, verifica se é vaído
		if (Crypto.isMacProtected(Configurations.SERVER_POLICY)) {
			System.out.println("Checking server policy file for integrity...");
			Crypto.hasValidMac(Configurations.SERVER_POLICY, serverpass);
			System.out.println("OK");

			// Caso contrario protege
		} else {
			byte[] groupsFile = Crypto.calculateFileMAC(serverpass, Configurations.SERVER_POLICY);
			Crypto.writeMacFile(groupsFile, Configurations.SERVER_POLICY);
		}

		System.out.println("File integrity verifications: PASSED");
		System.out.println("\n\n==============");
		System.out.println("SERVER STARTED");
		System.out.println("==============");

		// *****************************************************************************
		// SERVER MAIN LOOP
		// *****************************************************************************

		// Server Main - Client reception and thread creation
		while (true) {
			try {
				Socket inSoc = sSoc.accept();

				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// *****************************************************************************

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("Client connected from " + socket.getInetAddress());

			SSLSession session = ((SSLSocket) inSoc).getSession();

			System.out.println("Peer host is " + session.getPeerHost());
			System.out.println("Cipher is " + session.getCipherSuite());
			System.out.println("Protocol is " + session.getProtocol());
			System.out.println("Session created in " + session.getCreationTime());
			System.out.println("Session accessed in " + session.getLastAccessedTime());
		}

		// Run the thread
		public void run() {

			try {
				// OPEN STREAMS
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

				String user = null;
				// END OPEN STEAMS

				// RECEIVE CREDENTIALS
				try {
					user = (String) in.readObject();

					System.out.println("Connection from " + socket.getInetAddress());

				} catch (ClassNotFoundException e1) {
					System.err.println("Error receiving credentials");
					// e1.printStackTrace();
				}
				// END RECEIVE CREDENTIALS

				// AUTHENTICATION
				boolean wasSuccessful = false;

				if (user.length() > 0) {
					wasSuccessful = Authentication.authenticateUser(in, out, user, serverpass);
				}

				// END AUTHENTICATION

				if (wasSuccessful) {
					try {
						String flag = (String) in.readObject();
						String[] argArray = (String[]) in.readObject();
						String destination;

						// Procede according to flag sent
						switch (flag) {
						case "-m":
							System.out.println("Message To: " + argArray[0]);

							// 1 - Enviar a lista de membros
							Communication.sendFileOrGroup(out, argArray[0], serverpass);

							destination = Files.getDestination(argArray[0], user, serverpass);

							if (destination != "") {

								Date date = new Date();
								SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm");

								String fileName = date.getTime() + ".msg";


								Files.receiveFile(in, out,
										Configurations.MESSAGES_FOLDER + "/" + destination, fileName);

								boolean writeMsgFile = in.readBoolean();
								
								if (writeMsgFile) {
									
									Message.writeMessage(argArray[1], user, destination);
								}
								
								Files.receiveCodedKeysAndWrite(in, fileName, destination);

								

							} else {

								// Send that is not a valid destination
								out.writeBoolean(false);
								out.flush();

								System.out.println("ERROR! Not a user or group!");
							}

							break;

						case "-f":

							// 1 - Enviar a lista de membros
							Communication.sendFileOrGroup(out, argArray[0], serverpass);

							System.out.println("File to: " + argArray[0] + " - " + argArray[1]);

							destination = Files.getDestination(argArray[0], user, serverpass);

							if (destination != "") {

								Files.receiveFile(in, out, Configurations.MESSAGES_FOLDER + "/" + destination,
										argArray[1]);

								boolean writeMsgFile = in.readBoolean();

								if (writeMsgFile) {
									Message.writeMessage(argArray[1], user, destination);
								}
								
								Files.receiveCodedKeysAndWrite(in, argArray[1], destination);
								
							} else {

								// Send that is not a valid destination
								out.writeBoolean(false);
								out.flush();

								System.out.println("ERROR! Not a user or group!");
							}
							break;

						case "-r":

							// TODO: Enviar os ficheiros de mensagem todos?
							// CIFRAR A MENSAGEM COM K
							// CIFRAR K COM A CHAVE PRIVADA DO SERVER
							// DECIFRAR COM A PUBLICA NO CLIENTE
							// DECIFRAR A MENSAGEM COM A CHAVE PUBLICA DO SERVER

							if (argArray.length == 0) {
								System.out.println("Send all communications");

								// LIST ALL FOLDERS THAT CONTAIN THE USER's
								// USERNAME

								String[] folderList = Files.listFolders(Configurations.MESSAGES_FOLDER);
								StringBuilder convo = new StringBuilder();
								for (String s : folderList) {

									if (s.matches(".+#.+")) {

										String[] tmp = s.split("#");

										// RECONSTRUCT CONVERSATION
										if (tmp[0].equals(user)) {

											convo.append(Message.buildConversation(tmp[1], user, true, serverpass));
										} else if (tmp[1].equals(user)) {
											convo.append(Message.buildConversation(tmp[0], user, true, serverpass));
										}
									}
								}

								// SEND
								out.writeObject(convo.toString());
								out.flush();

							} else if (argArray.length == 1) {

								String convo = Message.buildConversation(argArray[0], user, false, serverpass);

								// SEND
								out.writeObject(convo);
								out.flush();

							} else if (argArray.length == 2) {
								System.out.println("Get file " + argArray[1] + " in " + argArray[0]);

								// OPEN CONVERSATION FOLDER
								destination = Files.getDestination(argArray[0], user, serverpass);
								File file = new File(
										Configurations.MESSAGES_FOLDER + "/" + destination + "/" + argArray[1]);

								if (file.exists()) {
									out.writeBoolean(true);
									out.flush();
									Files.sendFile(in, out, Configurations.MESSAGES_FOLDER + "/" + destination + "/",
											argArray[1]);
								} else {
									out.writeBoolean(false);
									out.flush();
								}

							}
							break;

						case "-a":
							System.out.println("Adds user " + argArray[0] + " to group " + argArray[1]);
							Group.addGroup(argArray[0], argArray[1], user, out, in, serverpass);

							break;

						case "-d":
							System.out.println("Delete user " + argArray[0] + " from group " + argArray[1]);
							Group.removeFromGroup(argArray[0], argArray[1], user, serverpass);
							break;

						default:
							System.out.println("Incorrect parameters!\n");
						}

					} catch (IOException | ClassNotFoundException e) {
						System.out.println("No command was sent from client!");
						// e.printStackTrace();
					}
				}

				// END SERVER MAIN

				System.out.println("-=*=-=*=-=*=- END OF SESSION -=*=-=*=-=*=-");
				// SOCKT AND STREAM CLOSING
				out.close();
				in.close();

				socket.close();
				// END SOCKT AND STREAM CLOSING

			} catch (IOException e) {
				System.err.println("Error running server thread!");
				// e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}