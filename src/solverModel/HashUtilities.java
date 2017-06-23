/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Static methods to calculate SHA-256 hashes of files.
 * @author Frash
 */
public class HashUtilities {

    /**
     * Compute the SHA-256 digest for the file in the specified path.
     * @param path path to the file to compute the digest of
     * @return The message digest, in string of hexadecimals form
     * @throws FileNotFoundException if the path doesn't lead to a file
     * @throws NoSuchAlgorithmException if the algorithm SHA-256 isn't available
     * @throws IOException if there's a problem when reading the data bytes
     */
    public static String fileHash(String path) throws FileNotFoundException, NoSuchAlgorithmException, IOException{
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(path);
        
        byte[] dataBytes = new byte[1024];
        
        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();
        
        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<mdbytes.length;i++) {
    	  hexString.append(Integer.toHexString(0xFF & mdbytes[i]));
    	}
        
    	//System.out.println("Hex format : " + hexString.toString());
        return hexString.toString();
    }
}
