package myChain;

import java.util.ArrayList;
import java.util.Date;

public class Block {
	public String hash;
	public String previousHash;
	public String merkleRoot;
	public ArrayList<Transaction> transactions=new ArrayList<Transaction>();
	private long timeStamp;
	private int nonce;
	
	public Block(String previousHash) {
		this.previousHash=previousHash;
		this.timeStamp=new Date().getTime();
		this.hash=CalculateHash();
	}
	
	public String CalculateHash() {
		String calculatedhash=StringUtil.applySha256(previousHash+Long.toString(timeStamp)+Integer.toString(nonce)+merkleRoot);
		return calculatedhash;
	}
	
	public void mineBlock(int difficulty) {
		merkleRoot=StringUtil.getMerkleRoot(transactions);
		String target=new String(new char[difficulty]).replace('\0', '0');
		while(!hash.substring(0,difficulty).equals(target)) {
			nonce++;
			hash=CalculateHash();
		}
		System.out.println("Block Mined!!!: "+hash);
	}
	
	public boolean addTransaction(Transaction transaction) {
		if(transaction==null) return false;
		//Chekcking for double usage in penidng transactions
		for(TransactionInput input: transaction.inputs) {
			if(MyChain.pendingUTXOUsage.containsKey(input.transactionOutputId)) {
				System.out.println("#UTXO "+input.transactionOutputId+ "is already used in pending transaction: "+MyChain.pendingUTXOUsage.get(input.transactionOutputId).transactionId);
				return false;
			}
		}
		if(!transaction.processTransaction()) {
			System.out.println("Transaction failed to process. Discrded");
			return false;
		}
		for(TransactionInput input: transaction.inputs) {
			MyChain.pendingUTXOUsage.put(input.transactionOutputId,transaction);
		}
		transactions.add(transaction);
		System.out.println("Transaction Successfully added to Block");
		return true;
	}
	
	public void printBlock() {
		System.out.println();
		System.out.println("Block Timestamp: "+ this.timeStamp);
		System.out.println("Block Hash: "+this.hash);
//		System.out.println("Block Data: "+this.data);
		System.out.println("Block Previous Hash: "+this.previousHash);
		System.out.println();
	}
}
