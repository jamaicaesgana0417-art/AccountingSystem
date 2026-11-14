import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/**
 * Runs a screen's database reads off the Swing thread while its refresh
 * button shows the refreshing state. Only the newest load applies its result,
 * so a slow older read cannot overwrite the screen after a newer one.
 */
final class BackgroundLoad {

    @FunctionalInterface
    interface Work<T> {
        T run() throws Exception;
    }

    private final RefreshButton button;
    private int latest;

    BackgroundLoad(RefreshButton button) {
        this.button = button;
    }

    /**
     * Runs {@code work} in the background, then calls {@code onSuccess} or
     * {@code onFailure} on the Swing thread. Both callbacks may start another
     * load.
     */
    <T> void start(Work<T> work, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        final int ticket = ++latest;
        button.setRefreshing(true);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                if (ticket != latest) {
                    return;  // a newer load has taken over the screen
                }

                T value = null;
                Exception failure = null;
                try {
                    value = get();
                } catch (ExecutionException exception) {
                    failure = unwrap(exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failure = exception;
                }

                button.setRefreshing(false);
                if (failure == null) {
                    onSuccess.accept(value);
                } else {
                    onFailure.accept(failure);
                }
            }
        }.execute();
    }

    private static Exception unwrap(ExecutionException exception) {
        Throwable cause = exception.getCause();
        return cause instanceof Exception ? (Exception) cause : exception;
    }
}
