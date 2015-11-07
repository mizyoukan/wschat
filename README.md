# wschat

A chat application using WebSocket for training.

This project uses:

- [http-kit](http://www.http-kit.org/) for WebSocket Server
- [Chord](https://github.com/jarohen/chord) for WebSocket Client
- [re-frame](https://github.com/Day8/re-frame) for Reagent Framework
- [Compojure](https://github.com/weavejester/compojure) for Server Routing

## Development Mode

### Run server:

```
lein repl
```

### Run figwheel:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).

## Production Build

```
lein do clean, uberjar
```
