package io.muoncore.protocol.reactivestream.server;

import io.muoncore.Muon;

public class ReactiveStreamServer implements ReactiveStreamServerHandlerApi {

  private Muon muon;
  private ReactiveStreamServerStack stack;
  private PublisherLookup lookup;

  public ReactiveStreamServer(Muon muon) {
    this.muon = muon;
    lookup = new DefaultPublisherLookup();
    this.stack = new ReactiveStreamServerStack(lookup, muon.getCodecs(), muon.getConfiguration(), muon.getDiscovery());
    muon.getProtocolStacks().registerServerProtocol(stack);
  }

  @Override
  public PublisherLookup getPublisherLookup() {
    return lookup;
  }
}
