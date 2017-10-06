
var nodeUrl = require("url");
var uuid = require('uuid');
require('sexylog');
var _ = require("underscore")
var proto = require("./client-protocol")
var simpleapi = require("./client-simple-api")

var serviceName;
var protocols = [];
var protocolName = 'reactive-stream';

exports.create = function(muon) {

  var api = exports.getApi(muon.infrastructure().serviceName, muon.infrastructure());

  muon.addServerStack(api)

  muon.subscribe = function (remoteurl, params, callback, errorCallback, completeCallback) {
    return api.subscribe(remoteurl, params, callback, errorCallback, completeCallback);
  }
}

exports.getApi = function (name, infra) {
    serviceName = name;

    var api = {
        name: function () {
            return protocolName;
        },
        endpoints: function () {
          return [];
        },
        subscribe: function (remoteServiceUrl, params, clientCallback, errorCallback, completeCallback) {
            infra.getTransport().then(function(transport) {
                try {
                    logger.debug("Subscribing to " + remoteServiceUrl + " with params " + JSON.stringify(params))
                    var serviceRequest = nodeUrl.parse(remoteServiceUrl, true);
                    var targetService = serviceRequest.hostname
                    var transChannel = transport.openChannel(targetService, protocolName);
                    var targetStream = serviceRequest.path;
                    var args = params;
                    var protocol = proto.create(
                        subscriber,
                        transChannel,
                        targetService,
                        serviceName,
                        targetStream,
                        args);
                    protocol.start();
                } catch (e) {
                    logger.error("Error in stream subscription initialisation ", e)
                }
            });

            var subscriber = simpleapi.subscriber(clientCallback, errorCallback, completeCallback);
            return subscriber.control;
        },
        protocols: function (ps) {
            protocols = ps;
        }
    }
    return api;
}
