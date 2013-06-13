package android.netinf.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.netinf.node.api.Api;

public class ApiToServiceMap<S> {

    private Map<Api, Set<S>> mLocal;
    private Map<Api, Set<S>> mRemote;

    public ApiToServiceMap() {
        mLocal = new ConcurrentHashMap<Api, Set<S>>();
        mRemote = new ConcurrentHashMap<Api, Set<S>>();
    }

    public void addLocalService(Api source, S destination) {
        put(mLocal, source, destination);
    }

    public void addRemoteService(Api source, S destination) {
        put(mRemote, source, destination);
    }

    private void put(Map<Api, Set<S>> map, Api source, S destination) {
        if (!map.containsKey(source)) {
            map.put(source, new LinkedHashSet<S>());
        }
        map.get(source).add(destination);
    }

    public Set<S> getLocalServices(Api api) {
        return get(mLocal, api);
    }

    public Set<S> getRemoteServices(Api api) {
        return get(mRemote, api);
    }

    private Set<S> get(Map<Api, Set<S>> map, Api source) {
        if (map.containsKey(source)) {
            return map.get(source);
        } else {
            return Collections.emptySet();
        }
    }

    public Set<Api> getApis() {
        Set<Api> apis = new HashSet<Api>();
        apis.addAll(mLocal.keySet());
        apis.addAll(mRemote.keySet());
        return apis;
    }

}
