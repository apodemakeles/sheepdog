package apodemas.sheepdog.server.distributed;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-03-08 14:13
 **/
public interface RemotingService {
    Future<ClientRegistryResult> RegisterClient(ClientRegistryInfo info, Promise<ClientRegistryResult> promise);
    Future<ClientUnregistryResult> UnregisterClient(ClientUnregistryInfo info, Promise<ClientUnregistryResult> promise);
}
