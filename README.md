# GIMP (Group Ironman Plugin)

A plugin for a better group ironman experience.

## Description

GIMP, or Group Ironman Plugin, provides a feature set for group ironmen to improve the experience
and make it easier to track the progress of your fellow gimps. Currently, it includes a map tracking
feature for showing your companions' locations on the map, with plenty of other features incoming!

## Usage

GIMP has two parts: a client-side plugin, and a basic server. A simple Node server can be found
[here](https://github.com/davidvorona/gimp-server). Otherwise, a custom-built server can be used.

### My Node.js server

To set up my server, simply follow the instructions in its [README](https://github.com/davidvorona/gimp-server).
Be sure to set the `IP` and `port` in the plugin config to wherever you've decided to run
your server.

### Custom-built server

If you'd like to build your own server, it must satisfy a few requirements:
1. It must include the following HTTP endpoints:

```
GET /ping
Request
    body: empty
Response
    body (JSON): 
    {
        [username: String]: {
            x: Integer,
            y: Integer,
            plane: Integer
        }
        ...
    }
```

```
POST /broadcast
Request
    body (JSON):
    {
        name: String,
        x: Integer,
        y: Integer,
        plane: Integer
    }
Response
    body (JSON): success/fail message
```

2. The `/broadcast` endpoint should add the user's coordinates to a map keyed by the username.
3. The `/ping` endpoint should fetch the JSON-ready map of the user coordinates.
4. There must be some sort of mechanism for removing users from the map if a certain period has passed
since an update from their client.
5. If you choose to use websockets for more realtime updates, you must use an implementation of `socket.io`. The
websockets server must use socket.io's [request-response API](https://socket.io/docs/v3/emitting-events/#acknowledgements),
and listen at `ping` and `broadcast` for the same actions.

