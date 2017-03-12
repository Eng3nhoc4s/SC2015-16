package functionality;

/**
 * This class handles usage display of application
 */
public class Usage {

	/**
	 * Prints the usage menu
	 */
	public static void printUsage() {
		
		System.out.println("myWhats <localUser> <serverAddress> [ -p <password> ]\n"
						 + "                                    [ -m <contact> <message> ]\n"
						 + "                                    [ -f <contact> <file> ]\n"
						 + "                                    [ -r [contact] [file] ]\n"
						 + "                                    [ -a <user> <group> ]\n"
						 + "                                    [ -d <user> <group> ]\n");
	}
}
