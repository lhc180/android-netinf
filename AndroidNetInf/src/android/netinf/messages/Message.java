package android.netinf.messages;

public abstract class Message {

    private String mId;

    protected Message(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

}
