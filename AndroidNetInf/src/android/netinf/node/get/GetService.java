package android.netinf.node.get;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;


public interface GetService {

    public GetResponse perform(Get get);

}
