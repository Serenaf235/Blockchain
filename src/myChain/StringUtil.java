package myChain;
import java.security.*;
import java.util.ArrayList;
import java.util.Base64;
public class StringUtil {
	public static String applySha256(String input) {
		try {
			MessageDigest digest= MessageDigest.getInstance("SHA-256");
			byte[] hash=digest.digest(input.getBytes("UTF-8"));
			StringBuffer hexString=new StringBuffer();
			for(int i=0;i<hash.length;i++) {
				String hex= Integer.toHexString(0xff &hash[i]);
				if(hex.length()==1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getStringFromKey(Key key) {
		
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}
	
	//Applies ECDSA Signature and returns the result
	public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
		Signature dsa;//digital signature algorithm
		byte[] output=new byte[0];
		try {
			dsa=Signature.getInstance("ECDSA","BC");
			dsa.initSign(privateKey);//initialize signing process by saving the private key
			byte[] strByte=input.getBytes();
			dsa.update(strByte);//passing the data
			byte[] realSig=dsa.sign();
			output=realSig;//three stages: pass the private key, pass the data, generate the signature
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
		return output;
	}
	
	//Verify a String signature
	public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
		try {
			Signature ecdsaVerify=Signature.getInstance("ECDSA","BC");
			ecdsaVerify.initVerify(publicKey);
			ecdsaVerify.update(data.getBytes());
			return ecdsaVerify.verify(signature);
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getMerkleRoot(ArrayList<Transaction> transactions) {
		int count=transactions.size();
		ArrayList<String> previousTreeLayer=new ArrayList<String>();
		for(Transaction transaction:transactions) {
			previousTreeLayer.add(transaction.transactionId);
		}
		ArrayList<String> treeLayer=previousTreeLayer;
		while(count>1) {
			treeLayer=new ArrayList<String>();
			for(int i=1;i<=previousTreeLayer.size();i+=2) {
				if(i==previousTreeLayer.size()) {
					treeLayer.add(applySha256(previousTreeLayer.get(i-1)+previousTreeLayer.get(i-1)));
				}else {
					treeLayer.add(applySha256(previousTreeLayer.get(i-1)+previousTreeLayer.get(i)));	
				}
			}
			count=treeLayer.size();
			previousTreeLayer=treeLayer;
		}
		String merkleRoot=(treeLayer.size()==1)? treeLayer.get(0):"";
		return merkleRoot;	
	}
}
