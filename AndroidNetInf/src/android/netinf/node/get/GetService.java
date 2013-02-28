package android.netinf.node.get;

import android.netinf.common.Ndo;
import android.netinf.common.NetInfService;

public interface GetService extends NetInfService {

    public Ndo get(Ndo ndo);

}
