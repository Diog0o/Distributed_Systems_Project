package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.PriorityQueue;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ServerState {

	private class TakeQueueEntry implements Comparable<TakeQueueEntry>{
		private String pattern;
		private int seqNumber;
		private final Condition tuplePut = allLock.newCondition();
		private boolean safeBit = true;


		public TakeQueueEntry(String pattern, int seqNumber) {
			this.pattern = pattern;
			this.seqNumber = seqNumber;
		}

		//the caller should aquire the lock before
		public void waitTuplePut() {
			if(safeBit){
				safeBit = false;
			} else{
				throw new RuntimeException("This method should only be called once per TakeQueueEntry");
			}
			try {
				tuplePut.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public int compareTo(TakeQueueEntry other) {
			return Integer.compare(this.seqNumber, other.seqNumber);		
		}

	}







	private List<String> tuples;
	private int nextSeq = 1;
	// Queue for takes to wait in order
	private List<TakeQueueEntry> takeQueue;
	// Single Lock for all class methods
	private final ReentrantLock allLock = new ReentrantLock();
	// Conditional variable to signal that a new sequence number is available
	private final Condition newSeqNum = allLock.newCondition();
	// Conditional variable to signal that a new put was made (to signal awaiting reads)
	private final Condition newTuple = allLock.newCondition();

	public ServerState() {
		this.tuples = new ArrayList<String>();
		this.takeQueue = new ArrayList<>();
	}


	private void readWaitForTuple(){
		try {
			newTuple.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	//the caller should aquire the lock before
	private void waitForSequence(int seqNumber) {
		while (seqNumber != nextSeq) {
			try {
				newSeqNum.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	//the caller should aquire the lock before
	private void incrementSequence() {
		nextSeq++;
		newSeqNum.signalAll();
	}
	
	public void put(String tuple, int seqNumber) {
		allLock.lock();
		try{
			waitForSequence(seqNumber);
			tuples.add(tuple);
			TakeQueueEntry takeToAwake = getFirstMatchingTake(tuple);
			if (takeToAwake != null) {
				takeToAwake.tuplePut.signal(); //if there was a take waiting, we dont increment seq num, take does it for us
			}
			else{
				incrementSequence(); //here, no take was waiting, so we increment the seq num
			}

		}
		finally{
			allLock.unlock();
		}

	}

	public String take(String pattern, int seqNumber) {
		TakeQueueEntry takeEntry;
		allLock.lock();
		try{
			waitForSequence(seqNumber);	
			String tuple = getMatchingTupleInTS(pattern);
			if(tuple == null){
				incrementSequence(); //increment sequence for the put to proceed (put knows this and doesnt increment again. That incrementing will be done by this take, later)
				takeEntry = new TakeQueueEntry(pattern, seqNumber);
				takeQueue.add(takeEntry); //append to the end
			
				takeEntry.waitTuplePut();
				tuple = getMatchingTupleInTS(pattern);
				takeQueue.remove(takeEntry);
			}
			tuples.remove(tuple);
			incrementSequence();
			return tuple;
		}
		finally{
			allLock.unlock();
		}

	}

	//the caller should aquire the lock before
	private String getMatchingTuple(String pattern, List<String> space){
		for (String tuple : space) {
			if (tuple.matches(pattern)) {
				return tuple;
			}
		}
		return null;
	}

	//the caller should aquire the lock before
	private String getMatchingTupleInTS(String pattern) {
		return getMatchingTuple(pattern, this.tuples);
	}

	//the caller should aquire the lock before
	private TakeQueueEntry getFirstMatchingTake(String tuple) {
		for (TakeQueueEntry entry : takeQueue) {
			if (tuple.matches(entry.pattern)) {
				return entry;
			}
		}
		return null; // Return null if no match is found
	}

	
	public String read(String pattern) {
		allLock.lock();
		try{
			String tuple = getMatchingTupleInTS(pattern);
			while (tuple == null) {
				readWaitForTuple();
				tuple = getMatchingTupleInTS(pattern);
			}
			return tuple;
		}
		finally{
			allLock.unlock();
		}
	}

	public List<String> getTupleSpacesState() {
		allLock.lock();
		try{
			return this.tuples;
		}
		finally{
			allLock.unlock();
		}

	}
}