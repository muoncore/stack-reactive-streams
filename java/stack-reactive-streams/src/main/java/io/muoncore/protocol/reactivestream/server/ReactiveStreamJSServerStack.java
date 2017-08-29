package io.muoncore.protocol.reactivestream.server;

import io.muoncore.Muon;
import io.muoncore.channel.Channel;
import io.muoncore.channel.ChannelConnection;
import io.muoncore.channel.Channels;
import io.muoncore.descriptors.OperationDescriptor;
import io.muoncore.descriptors.ProtocolDescriptor;
import io.muoncore.descriptors.SchemaDescriptor;
import io.muoncore.message.MuonInboundMessage;
import io.muoncore.message.MuonOutboundMessage;
import io.muoncore.protocol.ServerJSProtocol;
import io.muoncore.protocol.ServerProtocolStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReactiveStreamJSServerStack implements ServerProtocolStack {

  public static String REACTIVE_STREAM_PROTOCOL = "reactive-stream";

  private Muon muon;
  private PublisherLookup publisherLookup;

  public ReactiveStreamJSServerStack(Muon muon, PublisherLookup publisherLookup) {
    this.muon = muon;
    this.publisherLookup = publisherLookup;
  }

  @Override
  public ChannelConnection<MuonInboundMessage, MuonOutboundMessage> createChannel() {
    Channel<MuonOutboundMessage, MuonInboundMessage> api2 = Channels.workerChannel("rrpserver", "transport");

    ServerJSProtocol proto = new ServerJSProtocol(muon, "rpc", api2.left());

//    proto.addTypeForDecoding("ServerRequest", ServerRequest.class);
//    proto.addPostDecodingDecorator(ServerRequest.class, serverRequest -> {
//      serverRequest.setCodecs(muon.getCodecs());
//      return serverRequest;
//    });


//    Function<ServerRequest, Consumer> function = (ServerRequest request) -> {
//      return (Consumer<ScriptObjectMirror>) callbackOnResponse -> {
//
//        RequestResponseServerHandler handler = handlers.findHandler(request);
//
//        handler.handle(new RequestWrapper() {
//          @Override
//          public ServerRequest getRequest() {
//            return request;
//          }
//
//          @Override
//          public void answer(ServerResponse response) {
//            log.info("Response has been generated");
//            callbackOnResponse.call(null, response);
//          }
//        });
//      };
//    };

//    proto.setState("getHandler", function);

    proto.start(ReactiveStreamJSServerStack.class.getResourceAsStream("/reactive-streams-server.js"));

    return api2.right();
  }

  @Override
  public ProtocolDescriptor getProtocolDescriptor() {

    List<OperationDescriptor> ops = publisherLookup.getPublishers().stream().map(
      pub -> new OperationDescriptor(pub.getName(), "[" + pub.getPublisherType() + "]")
    ).collect(Collectors.toList());

    return new ProtocolDescriptor(REACTIVE_STREAM_PROTOCOL, "Reactive Streaming", "Provides the semantics of the Reactive Stream API over a muon message protocol",
      ops);
  }

  @Override
  public Map<String, SchemaDescriptor> getSchemasFor(String endpoint) {
    return null;
  }
}
