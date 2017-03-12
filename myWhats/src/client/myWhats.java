package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.SecretKey;

import functionality.*;

/**
 * This class represents the client myWhats
 */
public class myWhats {

	public static void main(String[] args) {

		System.setProperty("javax.net.ssl.trustStore", Configurations.TRUSTSTORE_NAME);
		System.setProperty("javax.net.ssl.trustStorePassword", "storepass");
		
		
		try {
			// Verify the giver argument number
			if (args.length < 3) {

				System.err.println("Incorrect parameters!\n");

				// Print correct usage
				Usage.printUsage();

			} else {

				if (args[0].contains("#")) {
					System.err.println("Invalid username. Usernames cannot contain \'#\'");
					System.err.println("Exiting now...");
					System.exit(-1);
				}

				Socket sock = null;

				String user = "";
				String pass = "";

				// Automatic password handling
				if (args.length >= 4) {
					user = args[0];
					pass = args[3];

				}

				// NO PASSWORD PROVIDED
				if (args.length == 3) {

					// Check if -p
					if (args[2].equals("-p")) {

						@SuppressWarnings("resource")
						Scanner sc = new Scanner(System.in);

						user = args[0];

						while (pass.length() < 2) {

							System.out.print("Password: ");
							pass = sc.nextLine();

						}

						// Close scanner
						// sc.close();
						// Warn user only registering or login will happen
						System.out.println("\nNOTICE: Your only arguments are your username, "
								+ "server and password!\n" + "This will only register you or try to log you in.\n");

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);
							out.close();
							in.close();
							sock.close();
						}

					} else {

						System.err.println("Incorrect parameters!\n");
						Usage.printUsage();
					}

					// JUST THE PASSWORD
				} else if (args.length == 4 && args[2].equals("-p")) {
					// Warn user only registering or login will happen
					System.out.println("\nNOTICE: Your only arguments are your username, " + "server and password!\n"
							+ "This will only register you or try to log you in.\n");

					sock = Communication.connect(args[1]);
					if (sock != null) {
						ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
						Authentication.login(in, out, user, pass);
						out.close();
						in.close();
						sock.close();
					}

					// PASWORD AND -r (no args)
				} else if (args.length == 5) {

					if (args[4].equals("-r")) {

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);
							String[] snd = {};
							Communication.sendCommand(out, "-r", snd);

							System.out.println("\nTrying to receive the lastest communications...");

							// Receive result
							String convo = (String) in.readObject();
							System.out.println(convo);

							out.close();
							in.close();
							sock.close();
						}

					} else {

						System.err.println("Incorrect parameters!\n");
						Usage.printUsage();

					}

					// PASWORD AND -r ARG1
				} else if (args.length == 6) {

					if (args[4].equals("-r")) {

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to receive all communications with " + args[5] + "...");

							String[] snd = { args[5] };
							Communication.sendCommand(out, "-r", snd);

							// Receive the answer
							String convo = (String) in.readObject();
							System.out.println(convo);

							out.close();
							in.close();
							sock.close();
						}

					} else {

						System.err.println("Incorrect parameters!\n");
						Usage.printUsage();

					}

					// ALL OTHER FLAGS
				} else if (args.length == 7) {

					switch (args[4]) {

					// MESSAGE OPERATION
					case "-m":

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to send a message to " + args[5] + "...");

							if (args[6].length() > 0) {
								String[] snd = { args[5] };

								Communication.sendCommand(out, "-m", snd);
								
								//Construir a mensagem
								// Build the message
								StringBuilder sb = new StringBuilder();

								sb.append(user);
								sb.append("\n");
								sb.append(args[6]);
								sb.append("\n");

								Date date = new Date();
								SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd hh:mm");
								sb.append(ft.format(date));
								
								String message = sb.toString();
								
								//1 - Receber a lista de membros
								String [] memberList = Communication.receiveUserOrGroup(in);
															
								//Mensagem enviada para um utilizador
								if(memberList.length == 1){
									
									memberList = new String[] {memberList[0], user};
								}
																
								//2 - Assinatura digital do ficheiro em claro
								//????
								
								//3 - Chave simetrica AES aleatória K
								SecretKey key = Crypto.randomAESKey();
								
								//4 - Cifra mensagem com K
								byte [] cipheredMsg = Crypto.encryptMessage(message.getBytes(), key);
								
								//5 - 
								File tempMSG = new File("tmpMSG");
								FileOutputStream msgOut = new FileOutputStream(tempMSG);
								msgOut.write(cipheredMsg);
								msgOut.close();
								
								//5 - Envia para o servidor
								Files.sendFile(in, out, "", "tmpMSG");
								out.writeBoolean(false);
								out.flush();
								tempMSG.delete();
								
								
								//6 - Cifra-se K com as chaves publicas dos destinatarios
								//Obtidas da truststore e envia-se para o servidor

								byte[][] keys = Crypto.encryptWithPrivateKey(key.getEncoded(), memberList, key, Configurations.TRUSTSTORE_NAME);
								
								Files.sendCodedKeys(out, memberList, keys);
								
								
							} else {
								System.err.println("ERROR: Unable to send empty message.");
							}
							
							out.close();
							in.close();
							sock.close();
						}
						break;

					// FILE OPERATION
					case "-f":

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to send the file " + args[6] + " to " + args[5] + "...");

							// Check if file doesn't have a disallowed extension
							if (args[6].toLowerCase().endsWith(".mac") || args[6].toLowerCase().endsWith(".msg")) {
								System.err
										.println("WARNING: .mac and .msg extension files are not allowed to be sent.");
								System.err.println("File " + args[6] + " not sent.");

							} else {

								// Check if file exists
								File file = new File(args[6]);

								// Don't bother server if file doesn't exist
								// localy
								if (file.exists() && !file.isDirectory()) {

									String[] snd = { args[5], args[6] };

									Communication.sendCommand(out, "-f", snd);
									
									//1 - Receber a lista de membros
									String [] memberList = Communication.receiveUserOrGroup(in);
									
									//Mensagem enviada para um utilizador
									if(memberList.length == 1){
										
										memberList = new String[] {memberList[0], user};
									}
									
									//2 - Assinatura digital do ficheiro em claro
									//???
									
									//3 - Chave simetrica AES aleatória K
									SecretKey key = Crypto.randomAESKey();
									
									//4 - Cifra mensagem com K
									Crypto.encryptFile(args[6], key);
									
									//5 - Envia para o servidor
									Files.sendFile(in, out, "", args[6]);
									out.writeBoolean(true);
									out.flush();
									
									//6 - Cifra-se K com as chaves publicas dos destinatarios
									//Obtidas da truststore e envia-se para o servidor
									byte[][] keys = Crypto.encryptWithPrivateKey(key.getEncoded(), memberList, key, Configurations.TRUSTSTORE_NAME);
									
									Files.sendCodedKeys(out, memberList, keys);
									

								} else {
									System.out.println("File " + args[6] + " not found!");
								}
							}
							out.close();
							in.close();
							sock.close();
						}
						break;

					// REVIEW OPERATION
					case "-r":

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to receive file " + args[6] + " from " + args[5] + "...");

							String[] snd = { args[5], args[6] };
							System.out.println("-r " + args[5] + " " + args[6]);
							Communication.sendCommand(out, "-r", snd);

							// Check if DOWNLOADS FOLDER exist
							File downloadsFolder = new File(Configurations.DOWNLOAD_FOLDER);
							if (!downloadsFolder.exists()) {

								downloadsFolder.mkdir();
							}

							boolean fileExists = in.readBoolean();

							if (fileExists) {
								Files.receiveFile(in, out, Configurations.DOWNLOAD_FOLDER, snd[1]);
							} else {
								System.out.println("The file you requested does not exist!");
							}

							out.close();
							in.close();
							sock.close();
						}
						break;

					// GROUP ADD OPERATION
					case "-a":

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to add user " + args[5] + " to  group " + args[6] + "...");

							System.out.println("-a " + args[5] + " " + args[6]);
							String[] snd = { args[5], args[6] };
							Communication.sendCommand(out, "-a", snd);

							// receive response
							String rsp = (String) in.readObject();
							System.out.println(rsp);

							out.close();
							in.close();
							sock.close();
						}
						break;

					// GROUP REMOVE OPERATION
					case "-d":

						sock = Communication.connect(args[1]);
						if (sock != null) {
							ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
							ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
							Authentication.login(in, out, user, pass);

							System.out.println("\nTrying to remove user " + args[5] + " from group " + args[6] + "...");

							String[] snd = { args[5], args[6] };
							System.out.println("-d " + args[5] + " " + args[6]);
							Communication.sendCommand(out, "-d", snd);

							out.close();
							in.close();
							sock.close();
						}
						break;

					// INVALID PARAMETERS
					default:
						System.err.println("Incorrect parameters!\n");
						Usage.printUsage();
					}
				} else {
					System.err.println("Incorrect parameters!\n");
					Usage.printUsage();
				}
			}
			
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.err.println("Error during execution. Server might be down.");
		}
	}
}
