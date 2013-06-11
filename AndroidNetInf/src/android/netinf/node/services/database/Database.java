package android.netinf.node.services.database;

import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;

public interface Database {

    public PublishResponse perform(Publish publish);
    public GetResponse perform(Get get);
    public SearchResponse perform(Search search);

}
