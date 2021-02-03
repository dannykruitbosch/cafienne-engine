package org.cafienne.service.api.projection.slick;

import org.cafienne.akka.actor.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class TransactionMonitor<T extends SlickTransaction<?>> {
    private final static Logger logger = LoggerFactory.getLogger(TransactionMonitor.class);
    private final String type;
    private final Map<String, T> transactions = new HashMap();
    private long count = 0;
    private long completed = 0;
    private boolean changed = false;
    private long interval = CaseSystem.config().queryDB().transactionMonitor().interval();

    TransactionMonitor(String type) {
        this.type = type;
        if (interval > 0) {
            startMonitoring();
        }
    }

    void startMonitoring() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(interval);
                    if (changed) {
                        changed = false;
                        logger.info(type + "\t- total transactions " + count + ", completed: " + completed + ", open: " + transactions.size());
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Transaction monitor for the " + type + " is interrupted and stopped; probably shutting down system?");
            }
        }).start();
    }

    void remove(String id) {
        T transaction = transactions.remove(id);
        if (transaction == null) {
            logger.info(type + " is removing a transaction that is not registered (on id " + id + ")");
        } else {
            completed++;
            changed = true;
            logger.debug(type + " completed transaction on id " + id);
        }
    }

    T get(String id, String tenant, TransactionFactory<T> factory) {
        T current = transactions.get(id);
        if (current != null) {
            return current;
        } else {
            T transaction = factory.createTransaction(id, tenant);
            logger.debug(type + " creating new transaction on id " + id);
            count++;
            changed = true;
            transactions.put(id, transaction);
            return transaction;
        }
    }
}
