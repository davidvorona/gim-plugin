# GIMP (Group Ironman Plugin)

A plugin for a better group ironman experience.

## Description

GIMP, or Group Ironman Plugin, provides a feature set for group ironmen to improve the experience
and make it easier to track the progress of your fellow gimps. Currently, it includes a map tracking
feature for showing your companions' locations on the map, with plenty of other features incoming!

## Usage

GIMP has two parts: a client-side plugin, and a basic server. A simple Node server can be found
[here](https://github.com/davidvorona/gimp-server). Otherwise, a public server can be used (in development).

### My Node.js server

To set up my server, simply follow the instructions in its [README](https://github.com/davidvorona/gimp-server).
Be sure to set the server address in the plugin config to wherever you've decided to run your server.

### Public server (in development)

There is a public server in the works that any group can connect to. Performance *may* be affected
because anyone can connect to it, but it should be good enough for most users.

## It's Live!

| Feature | Description |
| :-------------: | :-------------: |
| ![Map Point](https://i.imgur.com/5X6I9zw.png)  | Icons on the map will follow your companions around!  |
| ![Panel](https://i.imgur.com/wp1WrOm.png)  | The side panel shows real-time info about your fellow gimps.  |
