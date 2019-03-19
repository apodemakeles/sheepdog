package apodemas.sheepdog.server.distributed;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-03-18 20:08
 **/
public class DefaultRemotingService implements RemotingService {
    @Override
    public Future<ClientRegistryResult> RegisterClient(ClientRegistryInfo info, Promise<ClientRegistryResult> promise) {
        return null;
    }

    @Override
    public Future<ClientUnregistryResult> UnregisterClient(ClientUnregistryInfo info, Promise<ClientUnregistryResult> promise) {
        return null;
    }
}
