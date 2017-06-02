package io.muoncore.reactivestream.server

import io.muoncore.Discovery
import io.muoncore.codec.Codecs
import io.muoncore.config.AutoConfiguration
import io.muoncore.protocol.reactivestream.server.ImmediatePublisherGenerator
import io.muoncore.protocol.reactivestream.server.PublisherLookup
import io.muoncore.protocol.reactivestream.server.ReactiveStreamServerStack
import org.reactivestreams.Publisher
import spock.lang.Specification

class ReactiveStreamServerStackSpec extends Specification {

    def "protocol descriptor is correct"() {

        def pub1 = Mock(Publisher)
        def discovery = Mock(Discovery)

        def lookup = Mock(PublisherLookup) {
            getPublishers() >> [
                    new PublisherLookup.PublisherRecord("simple", PublisherLookup.PublisherType.HOT, new ImmediatePublisherGenerator(pub1)),
                    new PublisherLookup.PublisherRecord("tombola", PublisherLookup.PublisherType.COLD, new ImmediatePublisherGenerator(pub1))
            ]
        }
        def codecs = Mock(Codecs)
        def config = new AutoConfiguration()

        def stack = new ReactiveStreamServerStack(lookup, codecs, config, discovery)

        expect:
        stack.protocolDescriptor.protocolScheme == "reactive-stream"
        stack.protocolDescriptor.operations.size() == 2
    }
}
