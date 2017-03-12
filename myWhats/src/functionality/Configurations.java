package functionality;

/**
 * The configurations class contains the definition of various paths and data sizes
 */
public class Configurations {

	//Credential file and group list file
	public final static String CREDENTIALS_FILENAME = "!credentialsFile";
	public final static String GROUPS_FILENAME = "!groupsFile";
	
	//User and group data saving destination
	public final static String USERS_FOLDER = "users";
	public final static String GROUPS_FOLDER = "groups";
	
	//Messages saving destination
	public final static String MESSAGES_FOLDER = "messages";
	
	//Path to files received from -r DEST FILENAME
	public final static String DOWNLOAD_FOLDER = "downloads";
	
	//Client policy file
	public final static String CLIENT_POLICY = "client.policy";
	
	//Server policy file
	public final static String SERVER_POLICY = "server.policy";
	
	//Server Keystore File
	public final static String KEYSTORE_NAME = "keystore.jks";
	
	//Client Truststore File
	public final static String TRUSTSTORE_NAME = "truststore.jks";
	
	//Data block size to send and receive files
	public final static int DATA_BLOCK = 1024;
}
