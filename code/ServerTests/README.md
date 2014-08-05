# ServerTests

This directory is essentially a playground for work on the networking side of the project.

Once requirements have been clarified, a `Server` directory will be created for work on actual project code.

## Transmission Options

There are several ways of getting the frames off Glass once they've been captured:

### HTTP request
	- Simple
	- Well supported

### Sockets
	- Less overhead - faster
	- Allows two-way communication
	- More flexible
	- Harder to implement well
	- More complex if we're using different languages at each end

### WebSockets
	- Part of HTML5
	- Commonly used in web applications to replace AJAX polling
	- Good support in JavaScript (obviously), Python, Node and to a lesser extent, Java

The best way depends on what we eventually want to be able to do:

	- Send data back to the same Glass unit
	- Do further processing and send data to other devices
	- Allow the server and Glass units to have a conversation ("send me frames y through z" "here they are _" etc.)
	- Do distributed processing of frames
	- *insert feature we haven't yet thought of here*

## Data Formats 

### JSON

[JSON](http://json.org/) is a compact format for holding data. Although trivial to use in JavaScript, unfortunately it is not designed to work with binary data, which makes using it problematic for us. We would have to encode images in strings, and then decode them at the other end.

	- See: [Binary Data in JSON String](http://stackoverflow.com/questions/1443158/binary-data-in-json-string-something-better-than-base64)

### Java Serialization

Instances of classes implementing Java's [`Serializable`](http://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html) interface can be automatically sent down a wire and resurrected at the other end, with very little work. This method requires the use of sockets and Java both on Glass and on the server.

The only clear clear disadvantages of this are the language constraints it imposes, and the fact that data has to be represented in objects. If lots of image data has to be loaded into memory, it could potentially cause problems on Glass. 

### Protocol Buffers

[Protocol Buffers](https://developers.google.com/protocol-buffers/docs/overview) are a platform-independent format for serializing structured data. Unlike JSON, binary data transmission is supported natively.

Protocol compilers are available for Java and Python, and there are several unofficial [JavaScript implementations](http://stackoverflow.com/a/6929169/2765666).
