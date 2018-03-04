package io.muoncore.reactivestream.integration

import io.muoncore.MultiTransportMuon
import io.muoncore.channel.impl.StandardAsyncChannel
import io.muoncore.codec.json.JsonOnlyCodecs
import io.muoncore.config.AutoConfiguration
import io.muoncore.liblib.reactor.rx.broadcast.Broadcaster
import io.muoncore.memory.discovery.InMemDiscovery
import io.muoncore.memory.transport.InMemTransport
import io.muoncore.memory.transport.bus.EventBus
import io.muoncore.protocol.Auth
import io.muoncore.protocol.reactivestream.client.ReactiveStreamClient
import io.muoncore.protocol.reactivestream.server.PublisherLookup
import io.muoncore.protocol.reactivestream.server.ReactiveStreamServer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class ReactiveStreamIntegrationSpec extends Specification {

  def discovery = new InMemDiscovery()
  def eventbus = new EventBus()

  def "can create a publisher and subscribe to it remotely"() {

    def data = []

    def b = Broadcaster.create()
    def sub2 = Broadcaster.create()

    sub2.consume {
      data << it
    }

    def muon1 = muon("simples")
    def muon2 = client("tombola")

    muon1.publishSource("somedata", PublisherLookup.PublisherType.HOT, b)

    when:
    muon2.subscribe(new URI("stream://simples/somedata"), new Auth(), sub2)

    sleep(100)

    and:
    5000.times {
      b.accept(["hello": "world"])
    }

    then:
    new PollingConditions(timeout: 20).eventually {
      data.size() == 5000
    }
  }

  def "subscribing to remote fails with onError"() {

    def data = []
    def errorReceived = false

    def sub = new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {}

      @Override
      void onNext(Object o) {}

      @Override
      void onError(Throwable t) {
        errorReceived = true
      }

      @Override
      void onComplete() {}
    }

    def muon1 = muon("simples")
    def muon2 = client("tombola")

    when:
    muon2.subscribe(new URI("stream://simples/BADSTREAM"), new Auth(), sub)

    then:
    new PollingConditions(timeout: 10).eventually {
      errorReceived
    }
  }

  def "data remains in order"() {

    def muon1 = muon("simples")
    def muon2 = client("tombola")

    StandardAsyncChannel.echoOut = true

    def data = []

    def b = Broadcaster.create()
    def sub2 = Broadcaster.create()

    sub2.consume {
      data << it.getPayload(Integer)
    }

    muon1.publishSource("somedata", PublisherLookup.PublisherType.HOT, b)

    sleep(4000)
    when:
    muon2.subscribe(new URI("stream://simples/somedata"), new Auth(), sub2)

    sleep(1000)

    def inc = 1

    and:
    200.times {
      println "Publish"
      b.accept(inc++)
    }
    sleep(5000)

    def sorted = new ArrayList<>(data).sort()

    then:

    data == sorted

    cleanup:
    StandardAsyncChannel.echoOut = false
  }

  def "client receives complete signal correctly"() {

    def muon1 = muon("simples")
    def muon2 = client("tombola")

    StandardAsyncChannel.echoOut = true
    def data = []

    Publisher pub = new Publisher() {
      @Override
      void subscribe(Subscriber s) {
        s.onSubscribe(new Subscription() {
          @Override
          void request(long n) {
            println "CLIENT REQUESTED DATA $n"
          }

          @Override
          void cancel() {
            println "CLIENT SENT CANCEL"
          }
        })
        Thread.start {
          sleep 500
          s.onComplete()
        }
      }
    }

    def completed = false

    Subscriber sub = new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {
        println "Received SUBSCRIBE"
      }

      @Override
      void onNext(Object o) {
        println "Received NEXT"
      }

      @Override
      void onError(Throwable t) {
        println "Received ERROR"
      }

      @Override
      void onComplete() {
        println "Received COMPLETE"
        completed = true
      }
    }

    muon1.publishSource("somedata", PublisherLookup.PublisherType.HOT, pub)

    sleep 500
    when:
    muon2.subscribe(new URI("stream://simples/somedata"), new Auth(), sub)

    then:

    new PollingConditions(timeout: 10).eventually {
      completed == true
    }

    cleanup:
    StandardAsyncChannel.echoOut = false
  }

  def "client receives ERROR signal correctly"() {

    def muon1 = muon("simples")
    def (muon2, muon2transport)= clientFull("tombola")

    StandardAsyncChannel.echoOut = true

    def data = []

    Publisher pub = new Publisher() {
      @Override
      void subscribe(Subscriber s) {
        s.onSubscribe(new Subscription() {
          @Override
          void request(long n) {
            println "CLIENT REQUESTED DATA $n"
          }

          @Override
          void cancel() {
            println "CLIENT SENT CANCEL"
          }
        })

      }
    }

    def error = false

    Subscriber sub = new Subscriber() {
      @Override
      void onSubscribe(Subscription s) {
        println "Received SUBSCRIBE"
      }

      @Override
      void onNext(Object o) {
        println "Received NEXT"
      }

      @Override
      void onError(Throwable t) {
        println "Received ERROR"
        error = true
      }

      @Override
      void onComplete() {
        println "Received COMPLETE"
      }
    }

    muon1.publishSource("somedata", PublisherLookup.PublisherType.HOT, pub)

    sleep 500
    when:
    muon2.subscribe(new URI("stream://simples/somedata"), new Auth(), sub)

    sleep(500)
    muon2transport.triggerFailure()

    then:

    new PollingConditions().eventually {
      error == true
    }

    cleanup:
    StandardAsyncChannel.echoOut = false
  }


  ReactiveStreamServer muon(name) {
    def config = new AutoConfiguration(serviceName: name)
    def transport = new InMemTransport(config, eventbus)

    def muon = new MultiTransportMuon(config, discovery, [transport], new JsonOnlyCodecs())

    new ReactiveStreamServer(muon)
  }

  ReactiveStreamClient client(name) {
    def (ret, _) = clientFull(name)
    return ret
  }

  def clientFull(name) {
    def config = new AutoConfiguration(serviceName: name)
    def transport = new InMemTransport(config, eventbus)

    def muon = new MultiTransportMuon(config, discovery, [transport], new JsonOnlyCodecs())

    [new ReactiveStreamClient(muon), transport]
  }
}
