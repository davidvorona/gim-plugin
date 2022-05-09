# GIMP (Group Ironman Plugin)

A plugin for a better group ironman experience.

## Description

GIMP, or Group Ironman Plugin, provides a feature set for group ironmen to improve the experience
and make it easier to track the progress of your fellow gimps. Currently, it includes a map tracking
feature for showing your companions' locations on the map as well as a side panel with real-time stats
and other info!

## Usage

GIMP has two parts: a client-side plugin, and a basic server. A simple Node server can be found
[here](https://github.com/davidvorona/gimp-server). Otherwise, a public server can be used.

### Public server

There is now a public server that any group can connect to! Performance *may* be affected
because anyone can connect to it, but it should be good enough for most users.

**The address for the public server is https://gimp-server.herokuapp.com; simply copy it into the server address config
field, and you're all set!**

*DISCLAIMER: using our public server involves submitting your IP address to a 3rd party server maintained by myself.
The server keeps a copy of relevant character data and that's it--if this is unacceptable to you, the server is open-source,
and you are welcome to host it yourself (see below).*

### Our Node.js server

To set up our server, simply follow the instructions in its [README](https://github.com/davidvorona/gimp-server).
Be sure to set the server address in the plugin config to wherever you've decided to run your server.

## It's Live!

| Feature | Description |
| :-------------: | :-------------: |
| ![Map Point](https://i.imgur.com/vaD8z90.png)  | Icons on the map will follow your companions around, with the option to enable our patented Marauder's Map<sup>TM</sup> technology!  |
| ![Panel](https://i.imgur.com/wp1WrOm.png)  | The side panel shows real-time info about your fellow gimps.  |
| ![Ping](https://i.imgur.com/tqvLTUh.png)  | Your fellow gimps can ping any tile using shift left-click! |
