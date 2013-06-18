package android.netinf.node.publish;

import java.util.LinkedList;
import java.util.List;

import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.node.api.Api;
import android.util.Log;

import com.google.common.collect.SetMultimap;

public class PublishController implements PublishService {

    public static final String TAG = PublishController.class.getSimpleName();

    private SetMultimap<Api, PublishService> mLocalServices;
    private SetMultimap<Api, PublishService> mRemoteServices;

    public PublishController(SetMultimap<Api, PublishService> local, SetMultimap<Api, PublishService> remote) {
        mLocalServices = local;
        mRemoteServices = remote;
    }

    @Override
    public PublishResponse perform(Publish publish) {

        // Reduce hop limit (unless this was a local request)
        if (!publish.isLocal()) {
            publish = new Publish.Builder(publish).consumeHop().build();
        }

        List<PublishResponse> responses = new LinkedList<PublishResponse>();

        // Publish to local services
        for (PublishService publishService : mLocalServices.get(publish.getSource())) {
            responses.add(publishService.perform(publish));
        }

        // Publish to remote services
        if (publish.getHopLimit() > 0) {
            for (PublishService publishService : mRemoteServices.get(publish.getSource())) {
                responses.add(publishService.perform(publish));
            }
        }

        // Decide aggregated response status
        for (PublishResponse response : responses) {
            if (response.getStatus().isError()) {
                PublishResponse publishResponse = new PublishResponse.Builder(publish).failed().build();
                Log.i(TAG, "PUBLISH " + publish + "\n-> " + publishResponse);
                return publishResponse;
            }
        }

        PublishResponse publishResponse = new PublishResponse.Builder(publish).ok().build();
        Log.i(TAG, "PUBLISH " + publish + "\n-> " + publishResponse);
        return publishResponse;

    }

}
