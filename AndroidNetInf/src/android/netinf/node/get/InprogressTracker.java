package android.netinf.node.get;

import java.util.HashSet;
import java.util.Set;

import android.netinf.messages.Get;

public class InprogressTracker {

    Set<String> inProgress = new HashSet<String>();

    public synchronized boolean tryToStart(Get get) {
        if (inProgress.contains(get.getId())) {
            return false;
        } else {
            inProgress.add(get.getId());
            return true;
        }

    }

    public synchronized void stop(Get get) {
        inProgress.remove(get.getId());
    }

}
