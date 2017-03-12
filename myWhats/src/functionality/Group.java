package functionality;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles group operations
 */
public class Group {

	/**
	 * Verifies if a groups exists in the groups file
	 * 
	 * @param groupName
	 *            - Name of the group to be searched for
	 * @param serverpass TODO
	 * @return True if group exists in groups file
	 */
	static boolean groupExists(String groupName, String serverpass) {

		//Check if groups file MAC is valid
		if (Crypto.hasValidMac(Configurations.GROUPS_FILENAME, serverpass)) {
			try {

				BufferedReader br = new BufferedReader(new FileReader(Configurations.GROUPS_FILENAME));

				String line;
				while ((line = br.readLine()) != null) {

					if (line.equals(groupName)) {
						br.close();
						System.out.println("Group exists in groups file!");
						return true;
					}
				}

				br.close();

			} catch (IOException e) {
				System.err.println("Error verifying if group exists!");
				// e.printStackTrace();
			} 
		}
		return false;
	}

	/**
	 * Checks if contact is a client or a group
	 * 
	 * @param input
	 *            - the contact's id
	 * @param serverpass TODO
	 * @return 1 if it's a user, 0 if it's a group and -1 in case of error
	 */
	public static int isUserOrGroup(String input, String serverpass) {

		try {
			// Check if is User
			if (User.userExists(input, serverpass) != "") {
				return 1;

				// Check if is group
			} else if (groupExists(input, serverpass)) {
				return 0;
			}

		} catch (IOException e) {
			System.err.println("Erro verifying if it's a user or a group!");
			// e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Creates a new group if it doesn't exist, or adds a member if it does
	 * 
	 * @param username
	 *            - Username of the user to be added
	 * @param groupName
	 *            - Name of the group to be created
	 * @param callingUser
	 *            - The user who invokes the method
	 * @param out
	 *            - ObjectOutputStream
	 * @param in
	 *            - ObjectInputStream
	 * @param serverpass TODO
	 * @return True if group was created/user added to group successfully, False
	 *         otherwise
	 * @throws IOException 
	 */
	public static boolean addGroup(String username, String groupName, String callingUser, ObjectOutputStream out,
			ObjectInputStream in, String serverpass) throws IOException {

		try {
			// Check if there's a user with the supplied group name
			if (User.userExists(groupName, serverpass).equals("")) {

				// Check if group already exists
				if (groupExists(groupName, serverpass)) {

					// If group exists, check if calling user is admin
					if (isAdmin(groupName, callingUser, serverpass)) {

						// Check if user to be added already exists in group
						// If it doesn't, add it!
						if (!isInGroup(groupName, username, serverpass) && !(User.userExists(username, serverpass).equals(""))) {

							if (Crypto.hasValidMac(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", serverpass)) {
								
								BufferedWriter bw = new BufferedWriter(
										new FileWriter(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", true));
								bw.append(username);
								bw.newLine();
								bw.close();
								out.writeObject("User added with sucess!");
								out.flush();
								
								//calculate new file mac
								Crypto.updateMAC(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", serverpass);
								
								return true;
							}else{
								return false;
							}

							// If it's already added to the group
						} else {
							out.writeObject("Failed to add " + username + " to group " + groupName
									+ "!\nUser doesn't exist or it's already on this group");
							out.flush();
							System.out.println("Failed to add " + username + " to group " + groupName
									+ "!\nUser doesn't exist or it's already on this group");
							return false;
						}

					// The user is not an admin
					} else {
						out.writeObject("Failed to add " + username + " to the group " + groupName
								+ ". You are not the admin of this group.");
						out.flush();
						System.out.println("Failed to add " + username + " to the group " + groupName
								+ ". You are not the admin of this group.");
						return false;

					}

				// The group doesn't exist
				} else {

					// Create group Folder
					File createFolder = new File(Configurations.MESSAGES_FOLDER + "/" + groupName);

					createFolder.mkdir();

					// Writes the group properties file
					BufferedWriter bw = new BufferedWriter(
							new FileWriter(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", true));

					// Check if user is trying to add himself
					// It that's the case, write only one line
					if (username.equals(callingUser)) {

						bw.append(username);
						bw.newLine();

						// If user not trying to add himself
					} else {
						// Adicionar linha do admin
						bw.append(callingUser);
						bw.newLine();

						
						if (Crypto.hasValidMac(Configurations.USERS_FOLDER + "/" + callingUser + ".cfg", serverpass)) {
							// Add group to calling user user file
							BufferedWriter callingUserPersonalFile = new BufferedWriter(
									new FileWriter(Configurations.USERS_FOLDER + "/" + callingUser + ".cfg", true));
							callingUserPersonalFile.write(groupName);
							callingUserPersonalFile.newLine();
							callingUserPersonalFile.close();
							
							Crypto.updateMAC(Configurations.USERS_FOLDER + "/" + callingUser + ".cfg", serverpass);
						}
						
						// Check if user to be added exists
						if (!User.userExists(username, serverpass).equals("")) {
							// If so add user to group list member
							bw.append(username);
							bw.newLine();

							if (Crypto.hasValidMac(Configurations.USERS_FOLDER + "/" + username + ".cfg", serverpass)) {
								// Add group to user file
								BufferedWriter usrPersonalFile = new BufferedWriter(
										new FileWriter(Configurations.USERS_FOLDER + "/" + username + ".cfg", true));
								usrPersonalFile.write(groupName);
								usrPersonalFile.newLine();
								usrPersonalFile.close();
								
								Crypto.updateMAC(Configurations.USERS_FOLDER + "/" + username + ".cfg", serverpass);
							}
							
							System.out.println("Group created succesfully");
							out.writeObject("Group created succesfully");
							out.flush();

							// User to eb added is not registered
						} else {

							out.writeObject("Group created but user " + username
									+ " could not be added because it doesn't exist");
							out.flush();
							System.out.println("Group created but user " + username
									+ " could not be added because it doesn't exist");
						}
					}

					bw.close();
					Crypto.updateMAC(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", serverpass);

					if (Crypto.hasValidMac(Configurations.GROUPS_FILENAME, serverpass)) {
						// Register group in groups file
						BufferedWriter gf = new BufferedWriter(new FileWriter(Configurations.GROUPS_FILENAME, true));
						gf.write(groupName);
						gf.newLine();
						gf.close();
						
						Crypto.updateMAC(Configurations.GROUPS_FILENAME, serverpass);
					}
				}

				// If the group to be created conflicts with an already
				// registered group/user
			} else {
				out.writeObject("Failed to create group! A user with this name already exists!");
				out.flush();
				System.out.println("Failed to create group! A user with this name already exists");
				return false;
			}

		} catch (IOException e) {
			System.out.println("Failed to create group/add new element!");
			out.writeObject("Failed to create group! A user with this name already exists!");
			out.flush();
			// e.printStackTrace();
		}

		return false;
	}

	/**
	 * Verifies if a user is admin of a group
	 * 
	 * @param groupName
	 *            - The name of the group for this query
	 * @param username
	 *            - The username of the client for this query
	 * @param serverpass TODO
	 * @return True if user is admin of group, otherwise, False
	 */
	private static boolean isAdmin(String groupName, String username, String serverpass) {

		if (Crypto.hasValidMac(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", serverpass)) {
			try {

				BufferedReader br = new BufferedReader(
						new FileReader(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg"));

				String line = br.readLine();

				if (line.equals(username)) {
					br.close();
					System.out.println("User " + username + " is Admin of group " + groupName);
					return true;
				}
				br.close();

			} catch (IOException e) {
				// e.printStackTrace();
				System.err.println("Error checking if admin of group!");
			} 
		}
		return false;
	}

	/**
	 * Verifies if user is in group
	 * 
	 * @param groupName
	 *            - The name of the group
	 * @param username
	 *            - The user's username
	 * @param serverpass TODO
	 * @return True if
	 */
	private static boolean isInGroup(String groupName, String username, String serverpass) {

		if (Crypto.hasValidMac(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", serverpass)) {
			try {

				BufferedReader br = new BufferedReader(
						new FileReader(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg"));

				String line;

				while ((line = br.readLine()) != null) {
					if (line.equals(username)) {
						br.close();
						System.out.println("User " + username + " is in group " + groupName);
						return true;
					}
				}
				br.close();

			} catch (IOException e) {
				System.err.println("Error verifying if user belongs to group!");
				// e.printStackTrace();
			} 
		}
		return false;
	}

	/**
	 * Removes an entry from a text file
	 * 
	 * @param filename
	 *            - File to be redacted
	 * @param entry
	 *            - Entry to redact
	 * @param serverpass TODO
	 * @return True if entry was successfully redacted
	 */
	public static boolean removeEntry(String filename, String entry, String serverpass) {

		if (Crypto.hasValidMac(filename, serverpass)) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(filename));
				StringBuilder sb = new StringBuilder();

				// Stripe the entry
				String line;
				while ((line = in.readLine()) != null) {
					// If it's not the entry we're looking for, append
					if (!line.equals(entry)) {
						sb.append(line);
						sb.append("\n");
					}
				}

				in.close();

				// Write the new file
				BufferedWriter out = new BufferedWriter(new FileWriter(filename));
				out.write(sb.toString());
				out.close();
				
				Crypto.updateMAC(filename, serverpass);

				return true;

			} catch (IOException e) {
				System.err.println("Error in file operations for entry removal!");
				// e.printStackTrace();
			} 
		}
		return false;

	}

	/**
	 * Removes a user from a group and cleans the respective configuration files
	 * 
	 * @param username
	 *            - The username of the user to be removed
	 * @param groupName
	 *            - The groups name
	 * @param callingUser
	 *            - The user who invokes this method
	 * @param serverpass TODO
	 * @return True if user was successfully removed, False otherwise
	 */
	public static boolean removeFromGroup(String username, String groupName, String callingUser, String serverpass) {

		try {
			// Verificar se quem chama o metodo e o admin do grupo
			if (isAdmin(groupName, callingUser, serverpass)) {
				// Se o utilizador existe e pertence ao grupo
				if (User.userExists(username, serverpass) != null && isInGroup(groupName, username, serverpass)) {

					// Se o utilizador a remover ´e admin apaga o grupo todo
					if (username.equals(callingUser)) {

						// Pega na lista de elementos do grupo e percorre-a
						BufferedReader in = new BufferedReader(
								new FileReader(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg"));

						// Remove as entradas do ficheiro pessoal
						String line;
						while ((line = in.readLine()) != null) {

							removeEntry(Configurations.USERS_FOLDER + "/" + line + ".cfg", groupName, serverpass);

						}

						// apaga o ficheiro de membros
						Files.removeFile(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg");
						
						Crypto.removeMAC(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg");

						in.close();
						return true;
						// Just delete the element
					} else {

						// Remover do ficheiro do grupo
						removeEntry(Configurations.GROUPS_FOLDER + "/" + groupName + ".cfg", username, serverpass);

						// remover do ficheiro pessoal
						removeEntry(Configurations.USERS_FOLDER + "/" + username + ".cfg", groupName, serverpass);

						return true;

					}

				} else {
					System.out.println("User " + username + "doesn't exist or is not a member of " + groupName);
					return false;
				}

			} else {
				System.out.println(callingUser + " is not an admin of " + groupName + " and cannot delete " + username);
				return false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Error removing user from group!");
			e.printStackTrace();
		}

		return false;
	}
	
	/**
	 * Returns a String array with all the members of the group
	 * @param groupFile - The path to the groups config file
	 * @return String array with members name, null in case of error
	 */
	public static String[] membersList(String groupFile){
		
		BufferedReader br;
		List<String> list = new ArrayList<String>();
		
			try {
				br = new BufferedReader(new FileReader (groupFile));
				
				String tmp;
				while((tmp = br.readLine()) != null){

					list.add(tmp);
				}
				
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	return list.toArray(new String[0]);
	}
}
