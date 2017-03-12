package functionality;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

	/**
	 * Writes a file with a mac
	 * 
	 * @param mac
	 *            - Mac to be written
	 * @param filename
	 *            - File to associate the mac with
	 */
	public static void writeMacFile(byte[] mac, String filename) {

		try {

			if (mac.length != 0) {
				FileOutputStream fos = new FileOutputStream(filename + ".mac", false);
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(mac);
				out.close();
				fos.close();
			} else {
				File f = new File(filename + ".mac");
				f.createNewFile();
			}

		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Reads the mac from a file
	 * 
	 * @param filename
	 *            - File tor ead the mac from
	 * @return Byte array with mac
	 */
	public static byte[] readMacFile(String filename) {

		byte[] mac = null;

		try {

			FileInputStream fis = new FileInputStream(filename + ".mac");
			ObjectInputStream out = new ObjectInputStream(fis);
			mac = (byte[]) out.readObject();
			out.close();
			fis.close();

		} catch (IOException | ClassNotFoundException e) {

			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		return mac;
	}

	/**
	 * Reads a file a generates a mac from it
	 * 
	 * @param password
	 *            - Password to associate to this mac
	 * @param filename
	 *            - File to have the mac calculated
	 * @return Byte with mac generated from file and password
	 */
	public static byte[] calculateFileMAC(String password, String filename) {

		byte[] digest = null;

		try {

			SecretKey key = new SecretKeySpec(password.getBytes(), "HmacSHA256");

			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);

			BufferedReader bf = new BufferedReader(new FileReader(filename));

			String tmp = "";
			while ((tmp = bf.readLine()) != null) {
				mac.update(tmp.getBytes("UTF-8"));
			}
			bf.close();
			digest = mac.doFinal();

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return digest;
	}

	/**
	 * Checks if a .mac file exists for filename
	 * 
	 * @param filename
	 *            - The file's name to be checked
	 * @return True if a mac file exists for filename File
	 */
	public static boolean isMacProtected(String filename) {

		File f = new File(filename + ".mac");

		return (f.exists() && !f.isDirectory());
	}

	/**
	 * Verifies if file has valid mac
	 * 
	 * @param filename
	 *            - The file's name
	 * @param serverpass
	 *            - The server running pass
	 * @return True if file is correct, System shutdown otherwise
	 */
	public static boolean hasValidMac(String filename, String serverpass) {

		// CALCULATE MAC
		byte[] credentialsMacDigest = Crypto.calculateFileMAC(serverpass, filename);

		// CHECK IF PASS FILE IS PROTECTED WITH MAC
		if (Crypto.isMacProtected(filename)) {

			byte[] storedMac = Crypto.readMacFile(filename);

			// IF SO OK
			if (Arrays.equals(credentialsMacDigest, storedMac)) {
				return true;

				// IF NOT ERROR AND CLOSE
			} else {
				System.err.println("Error! <" + filename + "> MAC mismatch! File might have been tampered with!");
				System.exit(-1);
			}

		} else {
			System.err.println(filename + " is not MAC protected. Cannot check for valid MAC");
		}

		return false;
	}

	/**
	 * Returns a random 6 digit integer
	 * 
	 * @return a random 6 digit integer
	 */
	public static int generateRandomSixDigit() {
		SecureRandom sr = new SecureRandom();

		return sr.nextInt(899999) + 100000;
	}

	/**
	 * Salts and hashes a password
	 * 
	 * @param password
	 *            - The password
	 * @param salt
	 *            - The salt
	 * @return - A salted and hashed password in base64
	 */
	public static String hashPassword(String password, int salt) {

		String sal = new Integer(salt).toString();
		String ingredientes = password + sal;

		SecretKey key = new SecretKeySpec(ingredientes.getBytes(), "HmacSHA256");

		String salteadinho = Base64.getEncoder().encodeToString(key.getEncoded());

		return salteadinho;
	}

	/**
	 * Calculates and updates a file's MAC, creates it if doesn't exist
	 * 
	 * @param filename
	 *            - The file to be Mac'd
	 * @param serverpass
	 *            - The server's pass
	 */
	public static void updateMAC(String filename, String serverpass) {

		byte[] mac = Crypto.calculateFileMAC(serverpass, filename);
		Crypto.writeMacFile(mac, filename);
	}

	public static void removeMAC(String filename) {
		Files.removeFile(filename);
	}

	static byte[] computeSignature(String filename, String certUser, String certPass, byte[] byteArray) {

		byte[] signature = null;

		try {
			// Start the digital signature algorithm with server's private key
			Signature sign = Signature.getInstance("SHA256withRSA");

			FileInputStream kfile = new FileInputStream(Configurations.KEYSTORE_NAME);
			KeyStore kstore = KeyStore.getInstance("JKS");

			kstore.load(kfile, "storepass".toCharArray());

			PrivateKey key = (PrivateKey) kstore.getKey(certUser, certPass.toCharArray());
			sign.initSign(key);

			if (filename != null && byteArray == null) {

				String tmp;
				BufferedReader br = new BufferedReader(new FileReader(filename));

				while ((tmp = br.readLine()) != null) {
					sign.update(tmp.getBytes());
				}

				br.close();

			} else if (filename == null && byteArray != null) {

				sign.update(byteArray);

			} else {

				return null;
			}

			signature = sign.sign();

		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return signature;
	}

	public static void writeSignature(byte[] signature, String filename) {

		try {

			FileOutputStream fos = new FileOutputStream(filename + ".sig", false);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(signature);
			out.close();
			fos.close();

		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean hasSignature(String filename) {

		File f = new File(filename + ".sig");

		return (f.exists() && !f.isDirectory());
	}

	/**
	 * 
	 * @return
	 */
	public static SecretKey randomAESKey() {

		SecretKey key = null;

		try {

			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			key = kg.generateKey();

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return key;
	}

	public static void encryptFile(String filename, SecretKey key){
		
		try {
			File file = new File(filename);
			if(file.length() == 0)
				return;
			
			// Initialize the encode cipher
			Cipher aes = Cipher.getInstance("AES");
			aes.init(Cipher.ENCRYPT_MODE, key);

			//Temporary File to Encode
			File tempFile = new File("encoding.temp");
			
			//Input 
			FileInputStream input = new FileInputStream(file);
			
			//Cypher file output
			FileOutputStream output = new FileOutputStream(tempFile);
			CipherOutputStream encryptedOut = new CipherOutputStream(output, aes);

			//Encrypt
			byte[] b = new byte[128];  
			int redBytes = input.read(b);
			
			while (redBytes != -1) {
				encryptedOut.write(b, 0, redBytes);
				redBytes = input.read(b);
			}
			
			encryptedOut.close();
			input.close();

			//Delete Original and rename encoded file
			file.delete();
			tempFile.renameTo(file);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error encoding file");
		}
		
	}
	
	
	public static byte[] encryptMessage(byte[] byteArray, SecretKey key){
		
		byte[] toReturn = null;
		
		try {
			
			// Initialize the encode cipher
			Cipher aes = Cipher.getInstance("AES");
			aes.init(Cipher.ENCRYPT_MODE, key);

			toReturn = aes.doFinal(byteArray);
			
		} catch (Exception e) {
			System.err.println("Error encoding file");
		}
		
		return toReturn;
	}
	
	public static byte[][] encryptWithPrivateKey(byte[] toEncrypt, String[] members, SecretKey key, String keystoreFile){
	
		byte[][] keyEncoded = new byte[members.length][];
		
		try {
			
		FileInputStream kfile = new FileInputStream(keystoreFile);
		KeyStore kstore = KeyStore.getInstance("JKS");
		
		kstore.load(kfile, "storepass".toCharArray());
		
		int index = 0;
		for (String s : members) {
			
			//PUBLICK KEY
			Certificate cert = kstore.getCertificate(s);
			Cipher rsa = Cipher.getInstance("RSA");
			PublicKey publicKey = cert.getPublicKey();
			rsa.init(Cipher.WRAP_MODE, publicKey);

			keyEncoded[index] = rsa.wrap(key);
			index++;
			
		}
		
		kfile.close();
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return keyEncoded;
	}

}
