package com.xiongyingqi.algorithm;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.*;

/**
 * There are two kinds of threads, oxygen and hydrogen. Your goal is to group these threads to form water molecules. There is a barrier where each thread has to wait until a complete molecule can be formed. Hydrogen and oxygen threads will be given a releaseHydrogen and releaseOxygen method respectfully, which will allow them to pass the barrier. These threads should pass the barrier in groups of three, and they must be able to immediately bond with each other to form a water molecule. You must guarantee that all the threads from one molecule bond before any other threads from the next molecule do.
 * <p>
 * In other words:
 * <p>
 * If an oxygen thread arrives at the barrier when no hydrogen threads are present, it has to wait for two hydrogen threads.
 * If a hydrogen thread arrives at the barrier when no other threads are present, it has to wait for an oxygen thread and another hydrogen thread.
 * We don’t have to worry about matching the threads up explicitly; that is, the threads do not necessarily know which other threads they are paired up with. The key is just that threads pass the barrier in complete sets; thus, if we examine the sequence of threads that bond and divide them into groups of three, each group should contain one oxygen and two hydrogen threads.
 * <p>
 * Write synchronization code for oxygen and hydrogen molecules that enforces these constraints.
 * <p>
 * <p>
 * <p>
 * Example 1:
 * <p>
 * Input: "HOH"
 * Output: "HHO"
 * Explanation: "HOH" and "OHH" are also valid answers.
 * Example 2:
 * <p>
 * Input: "OOHHHH"
 * Output: "HHOHHO"
 * Explanation: "HOHHHO", "OHHHHO", "HHOHOH", "HOHHOH", "OHHHOH", "HHOOHH", "HOHOHH" and "OHHOHH" are also valid answers.
 * <p>
 * <p>
 * Constraints:
 * <p>
 * Total length of input string will be 3n, where 1 ≤ n ≤ 30.
 * Total number of H will be 2n in the input string.
 * Total number of O will be n in the input string.
 */

public class H2O {
    private int hSize = 0;
    private final ReadWriteLock hLock = new ReentrantReadWriteLock();
    private final Lock hWriteLock = hLock.writeLock();
    private final Lock hReadLock = hLock.readLock();
    private final Condition hCondition = hWriteLock.newCondition();

//    private int oSize = 0;
    private final ReentrantReadWriteLock oLock = new ReentrantReadWriteLock();
    private final Lock oWriteLock = oLock.writeLock();
//    private final Lock oReadLock = oLock.readLock();
    private final Condition oCondition = oWriteLock.newCondition();


    public H2O() {

    }


    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {
//        System.out.println("hydrogen running...");
        // 如果H个数已经达到两个
        hReadLock.lock();
        try {
            if (hSize == 2) {
                hReadLock.unlock();
                hWriteLock.lock();
                try {
                    while (hSize == 2) {
                        hCondition.await();
                    }
                    hReadLock.lock();
                    releaseHydrogen.run();
                    hSize++;
                } finally {
                    hWriteLock.unlock();
                }
            } else {
                while (hSize >= 2) {
                    hReadLock.unlock();
                    hWriteLock.lock();
                    try {
                        hCondition.await();
                        hReadLock.lock();
                    } finally {
                        hWriteLock.unlock();
                    }
                }
                hReadLock.unlock();
                hWriteLock.lock();
                try {
                    releaseHydrogen.run();
                    hSize += 1;
                    if (hSize == 2) {
                        hWriteLock.unlock();
                        oWriteLock.lock();
                        try {
                            oCondition.signal();
                            hWriteLock.lock();
                        } finally {
                            oWriteLock.unlock();
                        }
                    }
                    hReadLock.lock();
                } finally {
                    hWriteLock.unlock();
                }

            }
        } finally {
            hReadLock.unlock();
        }

    }

    public void oxygen(Runnable releaseOxygen) throws InterruptedException {
//        System.out.println("oxygen running...");
        oWriteLock.lock();
        try {
            hReadLock.lock();
            try {
                while (hSize != 2) {
                    hReadLock.unlock();
                    oCondition.await();
                    hReadLock.lock();
                }
                hReadLock.unlock();
                hWriteLock.lock();
                try {
                    releaseOxygen.run();
                    hSize -= 2;
                    hCondition.signalAll();
                    hReadLock.lock();
                } finally {
                    hWriteLock.unlock();
                }
            } finally {
                hReadLock.unlock();
            }
        } finally {
            oWriteLock.unlock();
        }

    }

    public static void main(String[] args) {
        H2O h2O = new H2O();
        final BlockingQueue queue = new ArrayBlockingQueue(1000, true);
        Thread thread = new Thread(() -> {
            while (true) {
                Object poll = null;
                try {
                    poll = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                System.out.println(poll);
            }
        });
        thread.setName("printThread");
        thread.start();


        Runnable releaseHydrogen = () -> {
            try {
                queue.put("H");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable releaseOxygen = () -> {
            try {
                queue.put("O");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        Thread hydrogenThread = new Thread(() -> {
            while (true) {
                try {
                    h2O.hydrogen(releaseHydrogen);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        hydrogenThread.setName("hydrogenThread");
        new Thread(hydrogenThread).start();
        new Thread(hydrogenThread).start();
        new Thread(hydrogenThread).start();

        Thread oxygenThread = new Thread(() -> {
            while (true) {
                try {
                    h2O.oxygen(releaseOxygen);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        oxygenThread.setName("oxygenThread");
        new Thread(oxygenThread).start();
        new Thread(oxygenThread).start();
        new Thread(oxygenThread).start();

        oxygenThread.start();
        hydrogenThread.start();


    }
}
