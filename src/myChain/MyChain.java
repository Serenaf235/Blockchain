package myChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.security.*;
//import com.google.gson.GsonBuilder;

public class MyChain {
	
	public static int difficulty = 5;
	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	// list of all unspent transactions outputs in the blockchain
	public static HashMap<String,TransactionOutput> UTXOs = 
								new HashMap<String,TransactionOutput>(); 
	//Used to track pending UTXO claims for the double spending mechanism
	public static HashMap<String, Transaction> pendingUTXOUsage = new HashMap<>();

	public static float minimumTransaction = 0.1f;
	
	public static Wallet walletA;
	public static Wallet walletB;
	
	public static Transaction genesisTransaction;
	
	public static void main(String[] args) {
	    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

	    walletA = new Wallet();
	    walletB = new Wallet();
	    Wallet coinbase = new Wallet();

	    System.out.println("[TEST 1] Wallets created and keys encrypted.");

	    genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, new ArrayList<>());
	    genesisTransaction.generateSignature(coinbase.privateKey);
	    genesisTransaction.transactionId = "0";
	    genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
	    UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

	    Block genesis = new Block("0");
	    genesis.addTransaction(genesisTransaction);
	    addBlock(genesis);
	    System.out.println("");

	    Block block1 = new Block(genesis.hash);
	    System.out.println("[TEST 2] WalletA sends 40 to WalletB");
	    Transaction tx1 = walletA.sendFunds(walletB.publicKey, 40f);
	    if (tx1 != null) block1.addTransaction(tx1);
	    addBlock(block1);
	    System.out.println("");

	    System.out.println("[TEST 3] Double spending UTXO from tx1");
	    Block block2 = new Block(block1.hash);
	    Transaction doubleSpend = new Transaction(walletA.publicKey, walletB.publicKey, 10f, tx1.inputs);
	    doubleSpend.generateSignature(walletA.privateKey);
	    block2.addTransaction(doubleSpend);
	    addBlock(block2);
	    System.out.println("");

	    System.out.println("[TEST 4] Tampered transaction detection");
	    Transaction tamperedTx = new Transaction(walletA.publicKey, walletB.publicKey, 5f, new ArrayList<>());
	    tamperedTx.inputs.add(new TransactionInput(tx1.outputs.get(0).id));
	    tamperedTx.generateSignature(walletA.privateKey);
	    tamperedTx.value = 999f;
	    System.out.println("Signature valid after tampering: " + tamperedTx.verifySignature());
	    System.out.println("");

	    System.out.println("[TEST 5] Pending-level double spending with real UTXO conflict");

	    Block block3 = new Block(block2.hash);

	    // Step 1: Send a small amount from walletA to walletB and extract the UTXO
	    Transaction initialTx = walletA.sendFunds(walletB.publicKey, 10f);
	    if (initialTx != null) {
	        block3.addTransaction(initialTx);

	        // Step 2: Reuse the exact same input UTXO from initialTx in another transaction
	        ArrayList<TransactionInput> sameInputs = new ArrayList<>();
	        for (TransactionInput input : initialTx.inputs) {
	            sameInputs.add(new TransactionInput(input.transactionOutputId));
	        }

	        Transaction doubleSpendTx = new Transaction(walletA.publicKey, walletB.publicKey, 5f, sameInputs);
	        doubleSpendTx.generateSignature(walletA.privateKey);
	        block3.addTransaction(doubleSpendTx); // This should be rejected due to pending conflict
	    }

	    addBlock(block3);

	    System.out.println("");

	    System.out.println("[TEST 6] Spend all funds from WalletB and attempt overspend");
	    Block block4 = new Block(block3.hash);
	    Transaction tx4 = walletB.sendFunds(walletA.publicKey, walletB.getBalance());
	    if (tx4 != null) block4.addTransaction(tx4);
	    Transaction tx5 = walletB.sendFunds(walletA.publicKey, 5f);
	    if (tx5 != null) block4.addTransaction(tx5);
	    addBlock(block4);
	    System.out.println("");

	    System.out.println("[TEST 7] Load WalletA key and sign new transaction");
	    walletA.loadPrivateKey();
	    Transaction tx6 = walletA.sendFunds(walletB.publicKey, 5f);
	    if (tx6 != null) {
	        Block block5 = new Block(block4.hash);
	        block5.addTransaction(tx6);
	        addBlock(block5);
	    }
	    System.out.println("");

	    System.out.println("[TEST 8] Create transaction with no inputs");
	    Transaction tx7 = new Transaction(walletA.publicKey, walletB.publicKey, 10f, new ArrayList<>());
	    tx7.generateSignature(walletA.privateKey);
	    Block block6 = new Block(blockchain.get(blockchain.size() - 1).hash);
	    block6.addTransaction(tx7);
	    addBlock(block6);
	    System.out.println("");

	    System.out.println("[TEST 9] Forge input/output mismatch");
	    Transaction tx8 = walletA.sendFunds(walletB.publicKey, 15f);
	    if (tx8 != null && tx8.outputs.size() > 1) tx8.outputs.remove(1);
	    Block block7 = new Block(blockchain.get(blockchain.size() - 1).hash);
	    if (tx8 != null) block7.addTransaction(tx8);
	    addBlock(block7);
	    System.out.println("");

	    System.out.println("[TEST 10] Sign transaction with wrong private key");
	    Transaction tx9 = walletA.sendFunds(walletB.publicKey, 5f);
	    if (tx9 != null) {
	        tx9.generateSignature(walletB.privateKey);
	        System.out.println("Signature valid with wrong key: " + tx9.verifySignature());
	    }
	    System.out.println("");

	    System.out.println("[TEST 11] Fragmented UTXOs and multi-input aggregation");
	    for (int i = 0; i < 10; i++) {
	        Transaction t = walletA.sendFunds(walletB.publicKey, 1f);
	        if (t != null) {
	            Block b = new Block(blockchain.get(blockchain.size() - 1).hash);
	            b.addTransaction(t);
	            addBlock(b);
	        }
	    }
	    System.out.println("");

	    System.out.println("[TEST 12] Long chain test (10 additional blocks)");
	    Block current = new Block(blockchain.get(blockchain.size() - 1).hash);
	    for (int i = 0; i < 10; i++) {
	        if (walletB.getBalance() >= 0.5f) {
	            Transaction t = walletB.sendFunds(walletA.publicKey, 0.5f);
	            if (t != null) current.addTransaction(t);
	        }
	        addBlock(current);
	        current = new Block(current.hash);
	    }
	    System.out.println("");

	    System.out.println("\nFinal Blockchain Valid: " + isChainValid());
	    }


public static void addBlock(Block newBlock) {
	newBlock.mineBlock(difficulty);
	blockchain.add(newBlock);
	
	//remove used UTXOs form pending map
	for(Transaction tran: newBlock.transactions) {
		for(TransactionInput input: tran.inputs) {
			pendingUTXOUsage.remove(input.transactionOutputId);
		}
	}
} 

public static Boolean isChainValid() {
		Block currentBlock; 
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		// A temporary list of unspent transactions at a given block state.
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); 
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		//loop through blockchain to check hashes:
		for(int i=1; i < blockchain.size(); i++) {
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i-1);
			//compare registered hash and calculated hash:
			if(!currentBlock.hash.equals(currentBlock.CalculateHash()) ){
				System.out.println("Current Hashes not equal");			
				return false;
			}
			//compare previous hash and registered previous hash
			if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
				System.out.println("Previous Hashes not equal");
				return false;
			}
			//check if hash is correctly solved
			if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
				System.out.println("This block hasn't been mined");
				return false;
			}
			
			//loop through the transaction of currentBlock:
			TransactionOutput tempOutput;
			for(int t=0; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Signature on Transaction(" + t + ") is Invalid");
					return false; 
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are note equal to outputs in Transaction(" + t + ")");
					return false; 
				}
				
				for(TransactionInput input: currentTransaction.inputs) {	
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);
				}
				
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
					System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
					return false;
				}
				if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
					return false;
				}
				
			}
		}
		return true;
	}
}