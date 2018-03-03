

module.exports = function(api) {

  return {
    fromTransport: function (msg) {
      log.info("GOT A MESSAGE! {}", msg)
    }
  };
}
