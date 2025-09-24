This is a first PoC implementation.
It is (conceptually) almost working, but there are multiple problems with it:

1. The LiveMeasureContexts (and other classes) are placed in the API currently. This is
   not where it belongs, but this is the only package/project which is accessible from all
   contexts where it's required, which are
   a) The reporting plugin, containing the service to receive measures, and forward them to
   b) The CallFunctionHandler, which sets up the context and receives measures in order to append them to the correct ReportNode
   c) The FunctionMessageHandler, which receives the information about where to send live measures
   d) finally, the actual function implementation (Keyword etc.), which acts as the source of the measures

2. The RestForwardingMeasureSink is already mostly prepared (i.e. it already receives the correct URL where it should be publishing measures), but the implementation is missing. That's because at this level, in turn we don't have the classes in scope to be able to serialize measures to JSON, and to send REST requests.
