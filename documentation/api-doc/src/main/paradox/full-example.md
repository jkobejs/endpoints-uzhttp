full-example
=========

Let's start with defining our domain model.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #domain-model }

After we have our domain defined let's describe HTTP API that we will use.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #endpoint-description }

We want to show our HTTP API documentation in Swagger UI so let's define OpenAPI documentation.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #endpoint-documentation }

Since we want to store our counter, retrieve it and increment it lets define service that will do that.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #counter-service }

If we want to bring out HTTP API descriptions to life we need to interpret them, so lets do that.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #endpoint-implementation }

We also need to interpret documentation endpoints, we will do that by using `uzhttp` implementation of `Assets` algebra.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #documentation-implementation }

All that is left is to serve our HTTP API is to run uzhttp server and provide layer with Counter's live environment, so let's to that.

@@snip [Counter.scala](/documentation/examples/documented/src/main/scala/counter/Counter.scala) { #main }
