package myChain;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;

public class Wallet {
	public PrivateKey privateKey;
	public PublicKey publicKey;
	public HashMap<String, TransactionOutput> UTXOs=new HashMap<String, TransactionOutput>();
	//AES key path
	private static final String KEY_PATH="wallet/secret.aes";
	//Encrypted private key path
	private static final String ENC_PRIV_PATH="wallet/encryptedPrivateKey.txt";
	
	public Wallet() {
		generateKeyPair();
	}
	public void generateKeyPair() {
		try {
			KeyPairGenerator keyGen=KeyPairGenerator.getInstance("ECDSA","BC");
			SecureRandom random= SecureRandom.getInstance("SHA1PRNG");
			ECGenParameterSpec ecSpec=new ECGenParameterSpec("prime192v1");
			keyGen.initialize(ecSpec,random);
			KeyPair keyPair=keyGen.generateKeyPair();
			privateKey=keyPair.getPrivate();
			publicKey=keyPair.getPublic();
			
			//Additional data for requirement 2
			SecretKey aesKey=CryptoUtils.generateKey();
			CryptoUtils.saveKey(aesKey, KEY_PATH);
			String encodedPrivKey= Base64.getEncoder().encodeToString(privateKey.getEncoded());
			String encryptedKey=CryptoUtils.encrypt(encodedPrivKey, aesKey);
			Files.write(Paths.get(ENC_PRIV_PATH), encryptedKey.getBytes());
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	//Also for requirement 2: Load Encrypted private key from file
	public void loadPrivateKey() {
		try {
			SecretKey aesKey= CryptoUtils.loadKey(KEY_PATH);
			String encrypted=new String(Files.readAllBytes(Paths.get(ENC_PRIV_PATH)));
			String decrypted=CryptoUtils.decrypt(encrypted, aesKey);
			byte[] keyBytes=Base64.getDecoder().decode(decrypted);
			PKCS8EncodedKeySpec keySpec= new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory kf=KeyFactory.getInstance("ECDSA","BC");
			privateKey=kf.generatePrivate(keySpec);
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public float getBalance() {
		float total=0;
		for(Map.Entry<String, TransactionOutput> item:MyChain.UTXOs.entrySet()) {
			TransactionOutput UTXO=item.getValue();
			if(UTXO.isMine(publicKey)) {
				UTXOs.put(UTXO.id, UTXO);
				total+=UTXO.value;
			}
		}
		return total;
	}
	
	public Transaction sendFunds(PublicKey Newrecipient, float value) {
		if(getBalance()<value) {
			System.out.println("#Not Enugh funds to send transaction. Transaction discarded. ");
			return null;
		}
		ArrayList<TransactionInput> inputs=new ArrayList<TransactionInput>();
		float total=0;
		for(Map.Entry<String, TransactionOutput> item:UTXOs.entrySet()) {
			TransactionOutput UTXO=item.getValue();
			total+=UTXO.value;
			inputs.add(new TransactionInput(UTXO.id));
			if(total>value) break;
		}
		Transaction newTransaction=new Transaction(publicKey,Newrecipient,value,inputs);
		newTransaction.generateSignature(privateKey);
		
		for(TransactionInput i: newTransaction.inputs) {
			i.UTXO= MyChain.UTXOs.get(i.transactionOutputId);
		}
		float leftOver=newTransaction.getInputsValue()-value;
		newTransaction.transactionId=newTransaction.calculateHash();
		
		newTransaction.outputs.add(new TransactionOutput(newTransaction.recipient, newTransaction.value, newTransaction.transactionId));
		newTransaction.outputs.add(new TransactionOutput(newTransaction.sender,leftOver,newTransaction.transactionId));
		
		for(TransactionInput input:inputs) {
			UTXOs.remove(input.transactionOutputId);
		}
		return newTransaction;
		
		
		
		
		
		
		
		
		
		
		
		
	}
}
