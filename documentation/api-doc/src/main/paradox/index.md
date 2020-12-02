endpoints-uzhttp
=========

@@@ index
* [Full Example](full-example.md)
@@@

[Endpoints](https://github.com/julienrf/endpoints) server interpreter backed by [uzhttp](https://github.com/polynote/uzhttp).

You Probably Shouldn't Use It Because You Probably Shouldn't Use uzhttp.
## Server
@@@vars
~~~ scala
"io.github.jkobejs" %% "endpoints-uzhttp-server" % "$version$"
~~~
@@@

@scaladoc[API documentation](endpoints4s.uzhttp.server.index)

### `Endpoints`

The `Endpoints` interpreter provides 
@scaladoc[intepret](endpoints4s.uzhttp.server.EndpointsWithCustomErrors$Endpoint)
and @scaladoc[interpretPure](endpoints4s.uzhttp.server.EndpointsWithCustomErrors$Endpoint)
methods that can be chained using `orElse` method and integrated to your uzhttp server.

For instance, given the following endpoint definition:

@@snip [EndpointsDocs.scala](/documentation/examples/basic/shared/src/main/scala/sample/algebra/DocumentedApi.scala) { #endpoint-definition }

It can be implemented as follows:

@@snip [EndpointsDocs.scala](/documentation/examples/basic/uzhttp-server/src/main/scala/sample/Api.scala) { #implementation }

The result is a partial function that can be integrated in your server like
any other partial function that satisfies given type definition using `uzhttp.server.Server`'s
[handleSome](https://github.com/polynote/uzhttp/blob/master/src/main/scala/uzhttp/server/Server.scala#L116) method.


### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, since interpreters return partial function of type 
```scala
PartialFunction[UzRequest, ZIO[R with Blocking, HTTPError, UzResponse]]
```
uzhttp server will return 404 error.

#### The incoming request is invalid

In that case, *endpoints* returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
@scaladoc[handleClientErrors](endpoints4s.uzhttp.server.EndpointsWithCustomErrors) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, *endpoints* returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
@scaladoc[handleServerError](endpoints4s.uzhttp.server.EndpointsWithCustomErrors) method.

