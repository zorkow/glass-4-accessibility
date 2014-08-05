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