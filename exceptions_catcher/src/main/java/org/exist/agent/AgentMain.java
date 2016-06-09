package org.exist.agent;

import org.exist.agent.transform.ClassTransformSpec;
import org.exist.agent.transform.CodeGenerator;
import org.exist.agent.transform.MethodAppender;
import org.exist.agent.transform.TransformerImpl;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class AgentMain {


    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {

        int port = 12251;

        if (arguments != null) {
            for (String argument : arguments.split(",")) {
                if (argument.startsWith("http=")) {
                    port = Integer.parseInt(argument.substring(argument.indexOf('=') + 1));
                }
            }
        }

        System.err.println("eXist-db agent installed");

        instrumentation.addTransformer(new TransformerImpl(createSpec()),true);
        instrumentation.retransformClasses(Throwable.class);

        //instrumentation.retransformClasses(...class);

        runHttpServer(port);
    }

    private static void runHttpServer(int port) throws IOException {
        final ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress("localhost", port));
        System.err.println("Serving broker leak info on http://localhost:"+port+"/");
        final ExecutorService es = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        es.submit(new Callable<Void>() {
            public Void call() throws Exception {
                while (true) {
                    final Socket s = ss.accept();
                    es.submit(new Callable<Void>() {
                        public Void call() throws Exception {
                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                // Read the request line (and ignore it)
                                String request = in.readLine();

                                PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
                                w.print("HTTP/1.0 200 OK\r\nContent-Type: text/plain;charset=UTF-8\r\n\r\n");

                                if ("GET /exceptions HTTP/1.1".equals(request)) {
                                    Listener.dumpExceptions(w);

                                } else if ("GET /reset HTTP/1.1".equals(request)) {
                                    Listener.reset();
                                    w.println("Done");

                                } else {
                                    Listener.dump(w);
                                }

                            } finally {
                                s.close();
                            }
                            return null;
                        }
                    });
                }
            }
        });
    }


    static List<ClassTransformSpec> createSpec() {
        return Arrays.asList(
            new ClassTransformSpec("org/exist/storage/DBBroker",
                new OccupationInterceptor(),
                new ReleaseInterceptor(),
                new Skip("<init>", "*"),
                new Skip("<clinit>", "*"),
                new Skip("initIndexModules", "*"),
                new Skip("addContentLoadingObserver", "*"),
                new Skip("clearContentLoadingObservers", "*"),
                new Skip("setId", "*"),
                new CheckerInterceptor()
            ),
            new ClassTransformSpec("java/lang/Throwable",
                new ConstructorInterceptor()
            )
        );
    }

    private static class ConstructorInterceptor extends MethodAppender {
        ConstructorInterceptor() {
            super("<init>", "*", false);
        }

        @Override
        protected void append(CodeGenerator g) {
            g.invokeAppStatic(Listener.class, "catchThrowable");
        }
    }

    private static class OccupationInterceptor extends MethodAppender {
        OccupationInterceptor() {
            super("incReferenceCount", "()V", false);
        }

        @Override
        protected void append(CodeGenerator g) {
            g.invokeAppStatic(Listener.class,"occupation",
                new Class[]{Object.class},
                new int[]{0});
        }
    }

    private static class ReleaseInterceptor extends MethodAppender {
        ReleaseInterceptor() {
            super("decReferenceCount", "()V", false);
        }

        @Override
        protected void append(CodeGenerator g) {
            g.invokeAppStatic(Listener.class,"release",
                new Class[]{Object.class},
                new int[]{0});
        }
    }

    private static class CheckerInterceptor extends MethodAppender {
        CheckerInterceptor() {
            super("*", "", false);
        }

        @Override
        protected void append(CodeGenerator g) {
            g.invokeAppStatic(Listener.class,"check",
                new Class[]{Object.class},
                new int[]{0});
        }
    }

    private static class Skip extends MethodAppender {
        Skip(String name, String signature) {
            super(name, signature, true);
        }

        @Override
        protected void append(CodeGenerator g) {
        }
    }
}
