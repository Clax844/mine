package fr.clax.aurel.ui;

/** Reversible timed transition. A long frame cannot overshoot the target. */
public final class Transition {
    private double progress;
    private boolean closing;
    public void close() { closing = true; }
    public boolean closing() { return closing; }
    public boolean finished() { return closing && progress <= 0; }
    public double update(double seconds, double durationMs, boolean enabled) {
        if (!enabled) progress = closing ? 0 : 1;
        else progress = Colors.clamp(progress + (closing ? -1 : 1) * Math.max(0, seconds) / (durationMs / 1000), 0, 1);
        return 1 - Math.pow(1 - progress, 3);
    }
}
