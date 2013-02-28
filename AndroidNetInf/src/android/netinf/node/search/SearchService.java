package android.netinf.node.search;

import java.util.Set;

import android.netinf.common.NetInfService;

public interface SearchService extends NetInfService {

    public void search(Search search, Set<String> tokens);

}
