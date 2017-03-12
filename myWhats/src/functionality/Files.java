package functionality;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class handles files and folder operations
 */
public class Files {

	/**
	 * Returns a String array with a list of folder names in path
	 * 
	 * @param path
	 *            - The path to map
	 * @return - String array with the folders in path
	 */
	public static String[] listFolders(String path) {

		File directory = new File(path);

		File[] folderList = directory.listFiles();
		List<String> list = new ArrayList<String>();

		for (File folder : folderList) {
			if (folder.isDirectory()) {
				list.add(folder.getName());
			}
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Returns a String array with a list of file names in path
	 * 
	 * @param path
	 *            - The path to map
	 * @return - String array with the file names in path
	 */
	public static String[] listFiles(String path) {

		File directory = new File(path);

		File[] fileList = directory.listFiles();
		List<String> list = new ArrayList<String>();

		if (fileList != null) {
			for (File file : fileList) {
				if (file.isFile()) {
					list.add(file.getName());
				}
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Creates the correct path according if destinatary is a group or a user
	 * 
	 * @param destinatary
	 *            - The destinatary
	 * @param callingUser
	 *            - The user who calls the method (remetent)
	 * @param serverpass TODO
	 * @return A#B destinatary is a user, B if destinatary is group, or "" if
	 *         error
	 */
	public static String getDestination(String destinatary, String callingUser, String serverpass) {

		int val = Group.isUserOrGroup(destinatary, serverpass);
		String destination = "";

		// Destinatary is user
		if (val == 1) {

			// WRITE A MESSAGE SHOWING A FILE WAS SENT
			StringBuilder dst = new StringBuilder();

			if (destinatary.compareTo(callingUser) <= 0) {

				dst.append(destinatary);
				dst.append("#");
				dst.append(callingUser);

			} else {

				dst.append(callingUser);
				dst.append("#");
				dst.append(destinatary);

			}

			destination = dst.toString();

			// Destinatary is group
		} else if (val == 0) {

			destination = destinatary;

		}

		return destination;
	}

	/**
	 * Deletes a folder and it's contents
	 * 
	 * @param folder
	 *            - A File object with the folder's path
	 */
	static void deleteFolder(File folder) {

		// List all files
		File[] files = folder.listFiles();

		if (files != null) {
			// Delete one by one
			for (File f : files) {

				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		// Delete folder
		folder.delete();
	}

	/**
	 * Sends a file
	 * 
	 * @param in
	 *            - ObjectInputStream
	 * @param out
	 *            - ObjectOutputStream
	 * @param filePath
	 *            - The path of the file
	 * @param fileName
	 *            - The file name
	 * @return True if sending was successful, False otherwise
	 */
	public static boolean sendFile(ObjectInputStream in, ObjectOutputStream out, String filePath, String fileName) {

		byte[] buffer = new byte[Configurations.DATA_BLOCK];
		FileInputStream file = null;

		try {
			
			file = new FileInputStream(filePath + fileName);

			// Asks if destination is valid
			boolean validDestination = in.readBoolean();

			if (validDestination) {

				// Asks if file exists remotely
				boolean fileExistsAtDestination = in.readBoolean();

				// If file doesn't exist send
				if (!fileExistsAtDestination) {
					// Send file size
					long fileSize = file.getChannel().size();
					out.writeLong(fileSize);
					out.flush();

					// Send file
					int count;
					while ((count = file.read(buffer)) > 0) {
						out.write(buffer, 0, count);
						out.flush();
					}
					file.close();

					// Receive received byte amount
					long rcvdBytes = in.readLong();

					if (rcvdBytes == fileSize) {
						System.out.println("File sent with success!");
						return true;
					} else {
						System.out.println("Failed to send file!");
						return false;
					}

				} else {
					file.close();
					System.out.println("File already exists!");
					return false;
				}

			} else {
				file.close();
				System.out.println("User or group doesn't exist!");
			}

		} catch (IOException e) {
			System.err.println("Error sending file!");
			e.printStackTrace();
		}

		try {
			file.close();
		} catch (IOException e) {
			// e.printStackTrace();
			System.err.println("File not found!");
		}
		return false;
	}

	/**
	 * Receives a file
	 * 
	 * @param in
	 *            - ObjectInputStream
	 * @param out
	 *            - ObjectOutputStream
	 * @param path
	 *            - The path to save the file
	 * @param fileName
	 *            - The file name
	 * @return True if file received successfully, False otherwise
	 */
	public static boolean receiveFile(ObjectInputStream in, ObjectOutputStream out, String path, String fileName) {

		byte[] buffer = new byte[Configurations.DATA_BLOCK];
		FileOutputStream file = null;
		String fullPath;

		try {
			
			// Answers if destination is valid
			if (path.equals("") || path == null) {
				fullPath = fileName;
			} else {
				fullPath = path + "/" + fileName;
			}

			File dest = new File(path);
			if (dest.exists() && dest.isDirectory()) {

				out.writeBoolean(true);
				out.flush();

				// Answers if file already exists
				File checkIfExists = new File(fullPath);

				if (checkIfExists.exists()) {
					System.out.println("File already exists locally!");
					out.writeBoolean(true);
					out.flush();
					return false;

				} else {
					System.out.println("Receive file!");
					out.writeBoolean(false);
					out.flush();

					// Receive file size
					long fileSize = in.readLong();

					// Receive file
					long recvd = 0;
					if (fileSize > 0) {

						file = new FileOutputStream(fullPath);

						int count;
						recvd = 0;
						while (recvd < fileSize) {
							count = in.read(buffer);
							file.write(buffer, 0, count);
							recvd += count;
						}

						file.close();
						
						
					}else{
						
						checkIfExists.createNewFile();
					}
					
					// Send received bytes amnount
					out.writeLong(recvd);
					out.flush();
					return true;
				}

			} else {
				System.out.println("User or group does not exist!");
				out.writeBoolean(false);
				out.flush();
				return false;
			}

		} catch (IOException e) {
			System.err.println("Error receiving file!");
			// e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Removes a file with filename
	 * @param filename - The path to the file to be removed
	 */
	public static void removeFile(String filename){
		File f = new File(filename);
		f.delete();
	}
	
	public static void sendCodedKeys(ObjectOutputStream out, String[] memberList, byte[][] keys){
		
		try {
			
			out.writeObject(memberList);
			out.flush();
			out.writeObject(keys);
			out.flush();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void receiveCodedKeysAndWrite(ObjectInputStream in, String filename, String destination){
		try {
			
			String[] memberList = (String[]) in.readObject();
			byte[][] keys = (byte[][]) in.readObject();
			int index = 0;
			
			for (String s : memberList) {
				
				File keyFile = new File(Configurations.MESSAGES_FOLDER + "/" + destination + "/" + filename + ".key." + s);
				FileOutputStream fos = new FileOutputStream(keyFile);
				fos.write(keys[index]);
				index++;
				fos.flush();
				fos.close();
			}
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
