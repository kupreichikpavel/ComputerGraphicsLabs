package by.bsu.imageproc.processing;

import java.util.function.DoubleConsumer;

public final class Progress {

    public static final Progress NONE = new Progress(null);

    private final DoubleConsumer sink;
    private volatile int stages = 1;
    private volatile int stage;
    private volatile boolean cancelled;

    public Progress(DoubleConsumer sink) {
        this.sink = sink;
    }

    public void stages(int count) {
        stages = Math.max(1, count);
        stage = 0;
    }

    public void nextStage() {
        stage = Math.min(stage + 1, stages);
        report(0);
    }

    void report(double fractionOfStage) {
        if (sink != null) {
            sink.accept(Math.min(1.0, (stage + fractionOfStage) / stages));
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public void checkCancelled() {
        if (cancelled) {
            throw new CancelledException();
        }
    }


    public static final class CancelledException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CancelledException() {
            super("Обработка отменена");
        }
    }
}
