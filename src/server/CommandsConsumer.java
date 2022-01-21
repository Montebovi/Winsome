package server;

import shared.utils.AppLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

// classe consumatore di oggetti CommandToExec.
// legge dalla coda passata sul costruttore.
// i task vengono mandati in esecuzione con il ExecutorService passato sul costruttore.
public class CommandsConsumer implements Runnable {

    private final BlockingQueue<Runnable> queue;
    private final ExecutorService pool;

    public CommandsConsumer(ExecutorService pool, BlockingQueue<Runnable> queue){
        this.queue = queue;
        this.pool = pool;
    }

    @Override
    public void run() {
        CommandToExec cmdToExec=null;
        AppLogger.log("Internal CommandsConsumer started");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                cmdToExec = (CommandToExec) queue.take();
                try {
                    pool.execute(cmdToExec);
                }
                catch (RejectedExecutionException e) {
                    //note: non accadere mai se il pool è stato istanziato con CallerRunsPolicy
                    System.out.printf("Coda comandi piena.... comando perso.");
                }
            }
            catch(InterruptedException e) {
                AppLogger.log("Interruzione del main!");
                System.exit(1);
            }
        }
    }
}
