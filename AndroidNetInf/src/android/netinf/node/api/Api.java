package android.netinf.node.api;

public interface Api {

    public static final Api JAVA = new Api() {
        @Override
        public void stop() { }
        @Override
        public void start() { }
    };

    public void start();

    public void stop();

}
