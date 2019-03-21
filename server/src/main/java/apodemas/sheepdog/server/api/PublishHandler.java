package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.PublishMessageTemplate;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.sub.SubscriptionManager;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-21 22:10
 **/
public class PublishHandler  extends JSONGetRequestHandler {
    private final SubscriptionManager subscriptionManager;

    public PublishHandler(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handle(HttpContext context) {
        String topic = context.pathParams().get("topic");
        String msg = context.pathParams().get("msg");
        if(StringUtils.empty(topic)){
            BAD_REQUEST(context, "topic is required");
            return;
        }

        if(StringUtils.empty(msg)){
            BAD_REQUEST(context, "msg is required");
            return;
        }

        try {
            PublishMessageTemplate template = createTemplate(topic, msg);
            List<Session> sessions = subscriptionManager.getTopicSubSessions(topic);
            List<ClientSessionInfo> infos = new ArrayList<>();
            for (Session session : sessions) {
                session.publish(template);
                ClientSessionInfo info = new ClientSessionInfo(session.clientId(), null);
                infos.add(info);
            }

            context.json(infos);
        }catch (Exception e){
            INTERNAL_SERVER_ERROR(context, e);
        }
    }

    private PublishMessageTemplate createTemplate(String topic, String msg)
            throws UnsupportedEncodingException {
        byte[] bytes = msg.getBytes("utf-8");
        PublishMessageTemplate template = new PublishMessageTemplate();
        template.setRetained(false);
        template.setQos(MqttQoS.AT_LEAST_ONCE);
        template.setTopic(topic);
        template.setPayload(bytes);

        return template;
    }
}
