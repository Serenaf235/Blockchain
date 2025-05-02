package myChain;

public class TransactionInput {
	public String transactionOutputId;//the previous output that will be used
	public TransactionOutput UTXO;//the previous transaction itself
	public TransactionInput(String transactionOutputId) {
		this.transactionOutputId=transactionOutputId;
	}
}
