package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-02-25 16:50
 **/
public interface SettingsAware {
    void initWithSettings(ServerSettings settings);
}
