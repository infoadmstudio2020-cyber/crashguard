package com.roni.library.crashguard;

/**
 * Callback listener interface invoked when an uncaught exception or handled crash is intercepted.
 */
public interface CrashListener {
    /**
     * Invoked immediately after a crash report is generated and before app exit / restart.
     *
     * @param thread The thread where the exception occurred.
     * @param throwable The unhandled throwable.
     * @param report The generated structured crash report.
     */
    void onCrash(Thread thread, Throwable throwable, CrashReport report);
}
