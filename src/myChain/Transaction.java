package myChain;
import java.security.*;
import java.util.ArrayList;

public class Transaction {
	
	public String transactionId; // this is also the hash of the transaction.
	public PublicKey sender; // senders address/public key.
	public PublicKey recipient; // Recipients address/public key.
	public float value;
	// this is to prevent anybody else from spending funds in our wallet.
	public byte[] signature; 

	public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
	public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

	// a rough count of how many transactions have been generated.
	private static int sequence = 0; 
	
	// Constructor: 
		public Transaction(PublicKey from, PublicKey to, 
				float value,  ArrayList<TransactionInput> inputs) {
			this.sender = from;
			this.recipient = to;
			this.value = value;
			this.inputs = inputs;
		}
		
		// This Calculates the transaction hash (which will be used as its Id)
		public String calculateHash() {
			//increase the sequence to avoid 2 transactions having the same hash
			sequence++; 
			return StringUtil.applySha256(
					StringUtil.getStringFromKey(sender) +
					StringUtil.getStringFromKey(recipient) +
					Float.toString(value) + sequence
					);
		}
		
		//Signs all the data we don't wish to be tampered with.
		//Updated for reqirement 2
		public void generateSignature(PrivateKey privateKey) {
			try {
				String data = StringUtil.getStringFromKey(sender) + 
						StringUtil.getStringFromKey(recipient) + Float.toString(value);
				byte[] hashedData=SignatureUtils.applySHA256(data);
				this.signature = SignatureUtils.sign(privateKey,hashedData);	
			}catch(Exception e) {
				throw new RuntimeException(e);
			}
				
		}
		//also updated
		//Verifies the data we signed hasn't been tampered with
		public boolean verifySignature() {
			try {
				String data = StringUtil.getStringFromKey(sender) + 
						StringUtil.getStringFromKey(recipient) + Float.toString(value);
				byte[] hashedData=SignatureUtils.applySHA256(data);
				return SignatureUtils.verify(sender, hashedData, signature);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
			
		}
		//updated for requirement 3
		public boolean processTransaction() {
			if(verifySignature() == false) {
				System.out.println("#Transaction Signature failed to verify");
				return false;
			}	
			//Double spending check: all inputs must exist
			for(TransactionInput i:inputs) {
				i.UTXO=MyChain.UTXOs.get(i.transactionOutputId);
				if(i.UTXO==null) {
					System.out.println("# Referenced input missing or already spent! (Double spending?)");
					return false;
				}
			}
			//Checks if transaction is valid:
			if(getInputsValue() < MyChain.minimumTransaction) {
				System.out.println("Transaction Inputs too small: " + getInputsValue());
				System.out.println("Please make sure the transaction amount is greater than " + 
										MyChain.minimumTransaction);
				return false;
			}
			//Generate outputs
			float leftOvers=getInputsValue()-value;
			transactionId=calculateHash();
			outputs.add(new TransactionOutput(this.recipient,value, transactionId));
			outputs.add(new TransactionOutput(this.recipient,leftOvers, transactionId));
			// add outputs to UTXO
			for(TransactionOutput o: outputs) {
				MyChain.UTXOs.put(o.id, o);
			}
			//remove spent inputs from UTXO
			for(TransactionInput i : inputs) {
				if(i.UTXO == null) continue;
				MyChain.UTXOs.remove(i.UTXO.id);
			}
			return true;
		}
			
		public float getInputsValue() {
			float total = 0;
			for(TransactionInput i : inputs) {
				if(i.UTXO != null) 
					total += i.UTXO.value;
			}
			return total;
		}
			
		public float getOutputsValue() {
			float total = 0;
			for(TransactionOutput o : outputs) {
				total += o.value;
			}
			return total;
		}
}
