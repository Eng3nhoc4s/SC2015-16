package functionality;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class handles User operations
 */
public class User {

	/**
	 * Adds a user to the credentials file
	 * 
	 * @param login
	 *            - Credentials to be added
	 */
	static boolean createUser(String login, String serverpass) {

		
		
		if (Crypto.hasValidMac(Configurations.CREDENTIALS_FILENAME, serverpass)) {
			
			try {

				// Insert credentials in the creds file
				BufferedWriter bw = new BufferedWriter(new FileWriter(Configurations.CREDENTIALS_FILENAME, true));

				bw.append(login);
				bw.newLine();
				bw.close();

				// Create user's personal folder
				String[] parseCreds = login.split(":");

				BufferedWriter uf = new BufferedWriter(
						new FileWriter(Configurations.USERS_FOLDER + "/" + parseCreds[0] + ".cfg", true));
				uf.close();

				Crypto.updateMAC(Configurations.USERS_FOLDER + "/" + parseCreds[0] + ".cfg", serverpass);
				
				System.out.println("New user registered");

				Crypto.updateMAC(Configurations.CREDENTIALS_FILENAME, serverpass);

				return true;

			} catch (IOException e) {

				System.err.println("Error opening credentials file for user insertion");
				// e.printStackTrace();
			} 
		}
		
		return false;
	}

	/**
	 * Verifies if a username is present in the credentials file
	 * 
	 * @param username
	 *            - User's username
	 * @param serverpass TODO
	 * 
	 * @return If username if present it's credentials will be returned
	 *         otherwise, empty will be returned
	 * @throws IOException
	 */
	static String userExists(String username, String serverpass) throws IOException {
		
		if (Crypto.hasValidMac(Configurations.CREDENTIALS_FILENAME, serverpass)) {
			
			try {

				BufferedReader br = new BufferedReader(new FileReader(Configurations.CREDENTIALS_FILENAME));

				String line;
				String[] credLine;
				while ((line = br.readLine()) != null) {

					credLine = line.split(":");

					if (credLine[0].equals(username)) {
						br.close();
						//System.out.println("Username exists in credentials file");
						return line;
					}
				}

				br.close();

			} catch (FileNotFoundException e) {

				System.err.println("Error opening credentials file for user verification");
				// e.printStackTrace();
			} 
		}
		
		System.out.println("Username <" + username + "> not found");
		return "";
	}
}
