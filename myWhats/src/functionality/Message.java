package functionality;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class handles Message operations
 */
public class Message {

	/**
	 * Builds a conversation from the individual message files
	 * 
	 * @param dest
	 *            - The destinatary
	 * @param callingUser
	 *            - The sender
	 * @param lastestOnly
	 *            - If it shows only the last message exchanged with
	 * @param serverpass TODO
	 * @return A string with the conversation
	 */
	public static String buildConversation(String dest, String callingUser, boolean lastestOnly, String serverpass) {

		// RECONSTRUCT CONVERSATION
		StringBuilder conversationBack = new StringBuilder();
		;
		try {
			System.out.println("Send all communications with " + dest);

			String destination = Files.getDestination(dest, callingUser, serverpass);

			String[] filesList = Files.listFiles(Configurations.MESSAGES_FOLDER + "/" + destination);

			if (filesList.length != 0) {
				// SHOW WHO IS THIS CONVERSATION WITH
				conversationBack.append("Contact: " + dest + "\n");

				if (lastestOnly) {
					String s = filesList[filesList.length - 1];

					if (s.matches("([^\\s]+(\\.(?i)(msg))$)")) {

						conversationBack.append(Message.parseMessage(
								Configurations.MESSAGES_FOLDER + "/" + destination + "/" + s, callingUser));

					}

				} else {

					for (String s : filesList) {

						if (s.matches("([^\\s]+(\\.(?i)(msg))$)")) {

							conversationBack.append(Message.parseMessage(
									Configurations.MESSAGES_FOLDER + "/" + destination + "/" + s, callingUser));
						}
					}
				}
			} else {
				conversationBack.append("No conversations found for user/group " + dest);
			}

		} catch (IOException e) {
			System.err.println("Error building conversation!");
			// e.printStackTrace();
		}
		return conversationBack.toString();
	}

	/**
	 * Presents a message propperly
	 * 
	 * @param path
	 *            - Path to the message file
	 * @param callingUser
	 *            - The sender
	 * @return A string with the parsed message
	 */
	public static String parseMessage(String path, String callingUser) throws IOException {

		BufferedReader br;
		StringBuilder sb = new StringBuilder();

		br = new BufferedReader(new FileReader(path));

		String tmp;
		int pos = 0;
		while ((tmp = br.readLine()) != null) {
			if (pos == 0) {
				if (tmp.equals(callingUser))
					sb.append("me: ");
				else
					sb.append(tmp + ": ");
			} else {
				sb.append(tmp);
				sb.append("\n");
			}
			pos++;
		}
		br.close();
		return sb.toString();
	}

	/**
	 * Receives and stores a message propperly
	 * 
	 * @param out
	 *            - Output Stream
	 * @param in
	 *            - Input Stream
	 * @param callingUser
	 *            - The user currently logged it to save who sent the message
	 * @param serverpass TODO
	 */
	public static void receiveMessage(ObjectOutputStream out, ObjectInputStream in, String contact, String callingUser,
			String serverpass) {

		try {

			String message = (String) in.readObject();
			
			String destination = Files.getDestination(contact, callingUser, serverpass);
			// -1 - not registered
			// 0 - group
			// 1 - user
			if (destination == "") {

				// Tells destination doesn't exist
				// out.writeBoolean(false);
				System.out.println("Message destinatary doesn't exist!");
				out.writeObject("Message destinatary doesn't exist!");
				out.flush();
			} else {

				writeMessage(message, callingUser, destination);
				System.out.println("Message sent from " + callingUser + " to " + contact);
				out.writeObject("Message sent from " + callingUser + " to " + contact);
				out.flush();
			}

		} catch (IOException e) {

			System.err.println("Error receiving message!");
			try {
				out.writeObject("Error sending message");
				out.flush();
			} catch (IOException e1) {
				// e1.printStackTrace();
			}
			// e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Writes a message to disk
	 * 
	 * @param message
	 *            - The message to be written
	 * @param callingUser
	 *            - The one who sends the message
	 * @param destination
	 *            - The one who receives the message
	 */
	public static void writeMessage(String message, String callingUser, String destination) throws IOException {
		System.out.println("Message: " + message);

		// Create a conversation folder
		File messagePath = new File(Configurations.MESSAGES_FOLDER + "/" + destination);
		if (!messagePath.exists()) {
			messagePath.mkdir();
		}

		Date date = new Date();

		// Write the message to disk
		BufferedWriter msg = new BufferedWriter(
				new FileWriter(Configurations.MESSAGES_FOLDER + "/" + destination + "/" + date.getTime() + ".msg"));
		msg.write(message);
		msg.close();
	}

	/************************************************************
	 * CLIENT SIDE
	 ************************************************************/

	/**
	 * Sends a message to the server
	 * 
	 * @param out
	 *            - Output Stream
	 * @param in
	 *            - Input Stream
	 * @param msg TODO
	 * @param destination
	 *            - The user or group for the message to be sent
	 * @param message
	 *            - The message to be sent
	 */
	public static void sendMessage(ObjectOutputStream out, ObjectInputStream in, Object msg) {

		try {
			out.writeObject(msg);
			out.flush();
			
			// Receive status from message delivery
			String result = (String) in.readObject();
			System.out.println(result);

		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Error sending message to server!");
			// e.printStackTrace();
		}
	}
}
