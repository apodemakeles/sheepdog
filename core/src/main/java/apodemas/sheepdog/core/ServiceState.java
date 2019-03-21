package apodemas.sheepdog.core;

/**
 * @author caozheng
 * @time 2019-03-21 09:20
 **/
public enum ServiceState {
    /**
     * Service just created,not start
     */
    CREATE_JUST,
    /**
     * Service Running
     */
    RUNNING,
    /**
     * Service shutdown
     */
    SHUTDOWN_ALREADY,
    /**
     * Service Start failure
     */
    START_FAILED
}
