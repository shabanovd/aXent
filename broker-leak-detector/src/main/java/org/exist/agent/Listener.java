package org.exist.agent;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class Listener {

    static class Record {
        final Stack<Exception> stackTrace = new Stack<Exception>();
        final Thread thread;
        final long time;

        Thread errorThread;
        long errorTime;
        Exception errorAt;

        Object obj;

        Record(Object obj) {
            this.obj = obj;
            this.thread = Thread.currentThread();
            this.time = System.currentTimeMillis();

            inc();
        }

        Record(Record r) {
            this.obj = r.obj;
            this.thread = r.thread;
            this.time = r.time;

            this.errorThread = r.errorThread;
            this.errorTime = r.errorTime;
            this.errorAt = r.errorAt;

            for (Exception ex : r.stackTrace) {
                this.stackTrace.push(ex);
            }
        }

        void dump(PrintWriter pw) {
            pw.println(thread.getName());
            int count = 0;
            for (Exception ex : stackTrace) {
                pw.println((count++)+": ");
                printout(ex, pw);
                pw.flush();
            }
        }

        void inc() {
            stackTrace.push(new Exception());

            if (Thread.currentThread() != thread) {
                error(Thread.currentThread()+": wrong thread on inc");
            }
        }

        boolean dec() {
            if (Thread.currentThread() != thread) {
                error(Thread.currentThread()+": wrong thread on dec");
            }

            stackTrace.pop();

            return stackTrace.isEmpty();
        }

        void check() {
            if (Thread.currentThread() != thread) {
                error(Thread.currentThread()+": wrong thread");
            }
        }

        private void error(String msg) {
            if (errorThread != null) return;

            errorThread = Thread.currentThread();
            errorAt = new Exception();
            errorTime = System.currentTimeMillis();

            if (ERRORS.size() <= 10) {
                ERRORS.add(new Record(this));
            }
            ERROR.println("ERROR: "+msg);
            dump(ERROR);
            System.err.println("======");
        }
    }

    private static Map<Object,Record> TABLE = new WeakHashMap<Object,Record>();
    private static List<Record> ERRORS = new ArrayList<Record>();

    static int THRESHOLD = 999999;

    static PrintWriter ERROR = new PrintWriter(System.err);

    public static synchronized void occupation(Object _this) {
        //println(Thread.currentThread()+": occupation "+_this);
        Record r = TABLE.get(_this);

        if (r == null) {
            put(_this, new Record(_this));
        } else {
            r.inc();
        }
    }

    private static synchronized void put(Object _this, Record r) {
        TABLE.put(_this, r);
        if(TABLE.size()>THRESHOLD) {
            THRESHOLD=999999;
            dump(ERROR);
        }
    }

    public static synchronized void release(Object _this) {
        //println(Thread.currentThread()+": release "+_this);
        Record r = TABLE.get(_this);
        if (r != null && r.dec()) {
            //println(Thread.currentThread()+": remove on release "+_this);
            TABLE.remove(_this);

            //printout(new Exception(), ERROR);
        }

        if (r == null) {
            println("ERROR??? "+Thread.currentThread());
        }
    }

    public static synchronized void check(Object _this) {
        //println(Thread.currentThread()+": check "+_this);

        Record r = TABLE.get(_this);
        if (r != null) {
            r.check();
        } else {
            StackTraceElement[] trace = (new Exception()).getStackTrace();

            //ignore case when under:
            // org.exist.storage.NativeBroker.<init>
            // org.exist.storage.BrokerPool.release
            for (StackTraceElement el : trace) {
                if (el.getClassName().equals("org.exist.storage.NativeBroker") && el.getMethodName().equals("<init>")) {
                    return;
                }
                if (el.getClassName().equals("org.exist.storage.BrokerPool") && el.getMethodName().equals("release")) {
                    return;
                }
            }

            r = new Record(_this);
            ERRORS.add(r);

            println(Thread.currentThread()+": ERROR here? "+_this);
            r.dump(ERROR);
        }
    }

    public static synchronized void dump(OutputStream out) {
        dump(new OutputStreamWriter(out));
    }

    static synchronized void dump(Writer w) {
        PrintWriter pw = new PrintWriter(w);
        Record[] records = TABLE.values().toArray(new Record[0]);
        pw.println(records.length+" brokers occupied");
        int i=0;
        for (Record r : records) {
            pw.println("#"+(++i));
            r.dump(pw);
        }
        pw.println("----");
        pw.flush();
    }

    static void printout(Exception ex, PrintWriter pw) {
        StackTraceElement[] trace = ex.getStackTrace();
        int i = 0;
        // skip until we find the Method.invoke() that called us
        for (; i < trace.length; i++)
            if (trace[i].getClassName().equals("java.lang.reflect.Method")) {
                i++;
                break;
            }
        // print the rest
        for (; i < trace.length; i++)
            pw.println("\tat " + trace[i]);
        pw.flush();
    }

    static void println(String msg) {
        ERROR.println(msg);
        ERROR.flush();
    }
}
