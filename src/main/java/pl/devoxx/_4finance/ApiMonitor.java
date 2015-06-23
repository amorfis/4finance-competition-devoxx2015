package pl.devoxx._4finance;

import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class ApiMonitor extends Observable {

    private final LoanApi loanApi;

    private Timer timer = new Timer();

    private State state = null;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ApiMonitor(LoanApi loanApi) {
        this.loanApi = loanApi;
    }


    public void doMonitor() {
        timer.scheduleAtFixedRate(new CheckingTask(), 0, 1000);
    }

    private void notifyIfNecessary(State newState) {
        if (newState != state) {
            state = newState;
            notifyObservers(state);
        }
    }

    class CheckingTask extends TimerTask {

        @Override
        public void run() {
            ExecutorService checkingExecutor = Executors.newSingleThreadExecutor();
            final Future future = checkingExecutor.submit(
                    () -> {
                        try {
                            loanApi.ping();
                            notifyIfNecessary(State.WORKING);
                        } catch (Exception e) {
                            notifyIfNecessary(State.BROKEN);
                        }
                    });
            checkingExecutor.shutdown(); //Won't kill running tasks.

            try {
                future.get(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
                notifyIfNecessary(State.BROKEN);
            }

            if (!checkingExecutor.isTerminated()) {
                checkingExecutor.shutdownNow();
            }
        }
    }

    public enum State {
        WORKING, BROKEN
    }

}
