# Endpoints uzhttp 

[Endpoints](https://github.com/julienrf/endpoints) server interpreter backed by [uzhttp](https://github.com/polynote/uzhttp).

You Probably Shouldn't Use It Because You Probably Shouldn't Use uzhttp.

See the [documentation](https://jkobejs.github.io/endpoints-uzhttp) to learn more.

## Running the Examples

~~~
$ ./sbt
> ++2.13.1
> <example>/run
~~~

Where `<example>` can be either
[`example-basic-uzhttp-server`](documentation/examples/basic/uzhttp-server) or
[`example-documented`](documentation/examples/documented).

And then browse http://localhost:8080. If you are running `example-documented` you can access swagger UI at http://localhost:8080/assets/index.html.
