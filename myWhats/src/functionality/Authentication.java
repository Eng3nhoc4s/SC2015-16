package functionality;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;

/**
 * This class handles the authentication process
 */
public class Authentication {

	/************************************************************
	 * CLIENT SIDE
	 ************************************************************/

	/**
	 * Authenticates with the server
	 * 
	 * @param in
	 *            - ObjectInputStream
	 * @param out
	 *            - ObjectOutputStream
	 * @param username
	 *            - User's username
	 * @param password TODO
	 * @return true if user was successfully logged in
	 */
	public static boolean login(ObjectInputStream in, ObjectOutputStream out, String username, String password) {

		boolean userExists = false;
		boolean usernameAvailable = false;
		boolean successLogin = false;
		int salt = 0;

		try {
			System.out.println("Trying to authenticate...");

			out.writeObject(username);
			out.flush();

			// Receive if user exists
			userExists = in.readBoolean();

			// If user is doesn't exist
			if (!userExists) {

				// Check if available
				usernameAvailable = in.readBoolean();

				if (usernameAvailable) {

					@SuppressWarnings("resource")
					Scanner read = new Scanner(System.in);
					String ans = "";

					// Keep prompting for a valid answer to register or not
					while (!ans.equals("n") && !ans.equals("y")) {
													
						System.out.println("This username is not registered. Do you want to register it? (Y/N)");
						ans = read.nextLine().toLowerCase();
						System.out.println(ans);
					}
					
					//read.close();

					if (ans.equals("y")) {
						out.writeBoolean(true);
						out.flush();
						
						//Receber o salt
						salt = in.readInt();
						
						//Saltear e enviar a pass
						String hashedPass = Crypto.hashPassword(password, salt);
						
						out.writeObject(hashedPass);
						out.flush();
						
					} else {
						out.writeBoolean(false);
						out.flush();
					}
				}

				successLogin = in.readBoolean();

				if (successLogin) {
					System.out.println("User registered successfully!");
				} else {
					System.out.println("User not registered!");
				}

			// Se o user existir verificar se foi logado com sucesso
			} else {
				
				//Receber o salt
				salt = in.readInt();
				
				//Saltear e enviar a pass
				String hashedPass = Crypto.hashPassword(password, salt);
				
				out.writeObject(hashedPass);
				out.flush();

				successLogin = in.readBoolean();

				if (successLogin) {
					System.out.println("Successfull Authentication!");
					return true;
				} else {
					System.out.println("Failed Authentication! Incorrect Credentials!");
					return false;
				}
			}

		} catch (IOException e) {

			System.err.println("Error during autentication!");
			// e.printStackTrace();
		}

		return false;
	}

	/************************************************************
	 * SERVER SIDE
	 ************************************************************/

	/**
	 * Authenticates a user - If user exists checks for password, if not,
	 * registers
	 * 
	 * @param in
	 *            - ObjectInputStream
	 * @param out
	 *            - ObjectOutputStream
	 * @param username
	 *            - Credentials supplied during login in format user:password
	 * @return True if password correct or new user was registered, False
	 *         otherwise
	 * @throws ClassNotFoundException 
	 */
	public static boolean authenticateUser(ObjectInputStream in, ObjectOutputStream out, String username, String serverpass) throws ClassNotFoundException {

		String creds;
		String pass;
		
		boolean register = false;

		try {
			// Verify if user exists (returns user:passhash:salt)
			if ((creds = User.userExists(username, serverpass)) != "") {

				// Send that the user exists
				out.writeBoolean(true);
				out.flush();

				String[] registeredCredentials = creds.split(":");
				
				//Send salt
				out.writeInt(Integer.parseInt(registeredCredentials[2]));
				out.flush();
				
				//Receive hashed password
				pass = (String) in.readObject();

				

				// If password matches the one registered
				if (registeredCredentials[1].equals(pass)) {
					System.out.println(registeredCredentials[0] + " logged in with success");
					out.writeBoolean(true);
					out.flush();
					return true;

				} else {

					System.out.println(registeredCredentials[0] + "'s password doesn't match");
					out.writeBoolean(false);
					out.flush();
					return false;
				}

				// User doesn't exist, register it
			} else {

				// Send that the user doesn't exist
				out.writeBoolean(false);
				out.flush();

				// Verify if a group exists with the same name
				if (!Group.groupExists(username, serverpass)) {

					// Tell username is available
					out.writeBoolean(true);
					out.flush();

					register = in.readBoolean();

					if (register) {
						
						int salt = Crypto.generateRandomSixDigit();
						
						//Send salt
						out.writeInt(salt);
						out.flush();
						
						//Receive hashed password
						pass = (String) in.readObject();
						
						boolean result = User.createUser(username + ":" + pass + ":" + salt, serverpass);
						out.writeBoolean(result);
						out.flush();
						return result;

					} else {
						System.out.println("User not registered!");
						out.writeBoolean(false);
						out.flush();
						return false;
					}

				} else {

					// Send username is not available
					out.writeBoolean(false);
					out.flush();
					return false;
				}
			}

		} catch (IOException e) {

			System.err.println("Error during authentication!");
			// e.printStackTrace();
		}

		return false;
	}
}
