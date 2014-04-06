/**
 * JDBM LICENSE v1.00
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "JDBM" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Cees de Groot.  For written permission,
 *    please contact cg@cdegroot.com.
 *
 * 4. Products derived from this Software may not be called "JDBM"
 *    nor may "JDBM" appear in their names without prior written
 *    permission of Cees de Groot.
 *
 * 5. Due credit should be given to the JDBM Project
 *    (http://jdbm.sourceforge.net/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE JDBM PROJECT AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * CEES DE GROOT OR ANY CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2000 (C) Cees de Groot. All Rights Reserved.
 * Contributions are Copyright (C) 2000 by their associated contributors.
 *
 * $Id: TransactionManager.java,v 1.1 2009/12/18 22:44:58 olivier_smadja Exp $
 */
package jdbm.recman;

import java.io.*;
import java.util.*;

/**
 *  This class manages the transaction log that belongs to every
 *  {@link RecordFile}. The transaction log is either clean, or
 *  in progress. In the latter case, the transaction manager
 *  takes care of a roll forward.
 *<p>
 *  Implementation note: this is a proof-of-concept implementation
 *  which hasn't been optimized for speed. For instance, all sorts
 *  of streams are created for every transaction.
 */
// TODO: Handle the case where we are recovering lg9 and lg0, were we
// should start with lg9 instead of lg0!
public final class TransactionManager {

    private RecordFile owner;

    // streams for transaction log.
    private FileOutputStream fos = null;
    private ObjectOutputStream oos = null;
    private boolean tlopen = false;
    /**
     * By default, we keep 10 transactions in the log file before
     * synchronizing it with the main database file.
     */
    static final int DEFAULT_TXNS_IN_LOG = 10;
    /**
     * Maximum number of transactions before the log file is
     * synchronized with the main database file.
     */
    private int _maxTxns = DEFAULT_TXNS_IN_LOG;
    /**
     * In-core copy of transactions. We could read everything back from
     * the log file, but the RecordFile needs to keep the dirty blocks in
     * core anyway, so we might as well point to them and spare us a lot
     * of hassle.
     */
    private ArrayList<BlockIo>[] txns = new ArrayList[DEFAULT_TXNS_IN_LOG];
    private int curTxn = -1;
    /** Extension of a log file. */
    static final String extension = ".lg";

    /**
     *  Instantiates a transaction manager instance. If recovery
     *  needs to be performed, it is done.
     *
     *  @param owner the RecordFile instance that owns this transaction mgr.
     */
    TransactionManager(RecordFile owner) throws IOException {
        this.owner = owner;
        recover();
    //open();
    }

    /**
     * Synchronize log file data with the main database file.
     * <p>
     * After this call, the main database file is guaranteed to be
     * consistent and guaranteed to be the only file needed for
     * backup purposes.
     */
    public void synchronizeLog()
            throws IOException {
        synchronizeLogFromMemory();
    }

    /**
     * Set the maximum number of transactions to record in
     * the log (and keep in memory) before the log is
     * synchronized with the main database file.
     * <p>
     * This method must be called while there are no
     * pending transactions in the log.
     */
    @SuppressWarnings("unchecked")
    public void setMaximumTransactionsInLog(int maxTxns)
            throws IOException {
        if(maxTxns <= 0) {
            throw new IllegalArgumentException(
                    "Argument 'maxTxns' must be greater than 0.");
        }
        if(curTxn != -1) {
            throw new IllegalStateException(
                    "Cannot change setting while transactions are pending in the log");
        }
        _maxTxns = maxTxns;
        txns = new ArrayList[maxTxns];
    }

    /** Builds logfile name  */
    private String makeLogName() {
        return owner.getFileName() + extension;
    }

    /** Synchs in-core transactions to data file and opens a fresh log */
    private void synchronizeLogFromMemory() throws IOException {
        if(tlopen) {
            close();

            TreeSet<BlockIo> blockList = new TreeSet<BlockIo>(new BlockIoComparator());

            int numBlocks = 0;
            int writtenBlocks = 0;
            for(int i = 0; i < _maxTxns; i++) {
                if(txns[i] == null) {
                    continue;
                }
                // Add each block to the blockList, replacing the old copy of this
                // block if necessary, thus avoiding writing the same block twice
                for(Iterator<BlockIo> k = txns[i].iterator(); k.hasNext();) {
                    BlockIo block = k.next();
                    if(blockList.contains(block)) {
                        block.decrementTransactionCount();
                    } else {
                        writtenBlocks++;
                        blockList.add(block);
                    }
                    numBlocks++;
                }

                txns[i] = null;
            }
            // Write the blocks from the blockList to disk
            synchronizeBlocks(blockList.iterator(), true);

            owner.sync();
        }
        curTxn = -1;
    //open();
    }

    /**
     * Opens the log file
     * This needs to be delayed 'till any actual write is done.
     * Currently it is not delayed, and causes unneeded disk writes for a db read-only.
     */
    private void open() throws IOException {
        fos = new FileOutputStream(makeLogName());
        oos = new ObjectOutputStream(fos);
        oos.writeShort(Magic.LOGFILE_HEADER);
        oos.flush();
        //curTxn = -1;
        tlopen = true;
    }

    /** Startup recovery on all files */
    @SuppressWarnings("unchecked")
    private void recover() throws IOException {
        String logName = makeLogName();
        File logFile = new File(logName);
        if(!logFile.exists()) {
            return;
        }
        if(logFile.length() == 0) {
            logFile.delete(); // @todo test for return and handle when file not deleted.
            return;
        }

        FileInputStream fis = new FileInputStream(logFile);
        ObjectInputStream ois = new ObjectInputStream(fis);

        try {
            if(ois.readShort() != Magic.LOGFILE_HEADER) {
                throw new Error("Bad magic on log file");
            }
        } catch(IOException e) {
            // corrupted/empty logfile
            logFile.delete(); // @todo test for return and handle when file not deleted.
            return;
        }

        while(true) {
            ArrayList<BlockIo> blocks = null;
            try {
                blocks = (ArrayList<BlockIo>)ois.readObject();
            } catch(ClassNotFoundException e) {
                throw new Error("Unexcepted exception: " + e);
            } catch(IOException e) {
                // corrupted logfile, ignore rest of transactions
                break;
            }
            synchronizeBlocks(blocks.iterator(), false);

            // ObjectInputStream must match exactly each
            // ObjectOutputStream created during writes
            try {
                ois = new ObjectInputStream(fis);
            } catch(IOException e) {
                // corrupted logfile, ignore rest of transactions
                break;
            }
        }
        owner.sync();
        logFile.delete(); // @todo test return code and handle when file not deleted.
    }

    /** Synchronizes the indicated blocks with the owner. */
    private void synchronizeBlocks(Iterator<BlockIo> blockIterator, boolean fromCore)
            throws IOException {
        // write block vector elements to the data file.
        while(blockIterator.hasNext()) {
            BlockIo cur = blockIterator.next();
            owner.synch(cur);
            if(fromCore) {
                cur.decrementTransactionCount();
                if(!cur.isInTransaction()) {
                    owner.releaseFromTransaction(cur, true);
                }
            }
        }
    }

    /** Set clean flag on the blocks. */
    private void setClean(ArrayList<BlockIo> blocks)
            throws IOException {
        for(Iterator<BlockIo> k = blocks.iterator(); k.hasNext();) {
            BlockIo cur = k.next();
            cur.setClean();
        }
    }

    /** Discards the indicated blocks and notify the owner. */
    private void discardBlocks(ArrayList<BlockIo> blocks)
            throws IOException {
        for(Iterator<BlockIo> k = blocks.iterator(); k.hasNext();) {
            BlockIo cur = k.next();
            cur.decrementTransactionCount();
            if(!cur.isInTransaction()) {
                owner.releaseFromTransaction(cur, false);
            }
        }
    }

    /**
     *  Starts a transaction. This can block if all slots have been filled
     *  with full transactions, waiting for the synchronization thread to
     *  clean out slots.
     */
    void start() throws IOException {
        curTxn++;
        if(curTxn == _maxTxns) {
            synchronizeLogFromMemory();
            curTxn = 0;
        }
        txns[curTxn] = new ArrayList<BlockIo>();
    }

    /**
     *  Indicates the block is part of the transaction.
     */
    void add(BlockIo block) throws IOException {
        block.incrementTransactionCount();
        txns[curTxn].add(block);
    }

    /**
     *  Commits the transaction to the log file.
     */
    void commit() throws IOException {
        if(!tlopen) {
            open();
        }
        oos.writeObject(txns[curTxn]);
        sync();

        // set clean flag to indicate blocks have been written to log
        setClean(txns[curTxn]);

        // open a new ObjectOutputStream in order to store
        // newer states of BlockIo
        oos = new ObjectOutputStream(fos);
    }

    /** Flushes and syncs */
    private void sync() throws IOException {
        if(tlopen) {
            oos.flush();
            fos.flush();
            fos.getFD().sync();
        }
    }

    /**
     *  Shutdowns the transaction manager. Resynchronizes outstanding
     *  logs.
     */
    void shutdown() throws IOException {
        if(tlopen) {
            synchronizeLogFromMemory();
            close();
            curTxn = -1;
        }
    }

    /**
     *  Closes open files.
     */
    private void close() throws IOException {
        if(tlopen) {
            sync();
            if(oos != null) {
                oos.close();
            }
            if(fos != null) {
                fos.close();
            }
        }
        oos = null;
        fos = null;
        tlopen = false;
    }

    /**
     * Force closing the file without synchronizing pending transaction data.
     * Used for testing purposes only.
     */
    void forceClose() throws IOException {
        if(oos != null) {
            oos.close();
        }
        if(fos != null) {
            fos.close();
        }
        oos = null;
        fos = null;
        curTxn = -1;
        tlopen = false;
    }

    /**
     * Use the disk-based transaction log to synchronize the data file.
     * Outstanding memory logs are discarded because they are believed
     * to be inconsistent.
     */
    void synchronizeLogFromDisk() throws IOException {
        close();

        for(int i = 0; i < _maxTxns; i++) {
            if(txns[i] == null) {
                continue;
            }
            discardBlocks(txns[i]);
            txns[i] = null;
        }

        recover();
        curTxn = -1;
    // open();
    }

    /** INNER CLASS.
     *  Comparator class for use by the tree set used to store the blocks
     *  to write for this transaction.  The BlockIo objects are ordered by
     *  their blockIds.
     */
    public static class BlockIoComparator
            implements Comparator<BlockIo> {

        public int compare(BlockIo block1, BlockIo block2) {
            int result = 0;
            if(block1.getBlockId() == block2.getBlockId()) {
                result = 0;
            } else if(block1.getBlockId() < block2.getBlockId()) {
                result = -1;
            } else {
                result = 1;
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

    } // class BlockIOComparator
}
