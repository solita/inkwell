# Inkwell

Inkwell is a companion library to [Quil](https://github.com/quil/quil). Quil
brings [Processing](http://processing.org/) to Clojure. Inkwell makes it fit
in by making it simple to program interactively with pure functions.

## A separation of purely functional logic and impure rendering

An Inkwell sketch consists of two parts:

1. `handle-event`, a pure function that calculates a new sketch state based on
   a previous state and an event. (That is, its type is `[State Event -> State]`).

2. `draw`, an impure function that takes a sketch state and renders it using
   Quil. (That is, its type is `[State -> Any]`).

This approach naturally leads to most of your code living on the purely
functional side, where it is easy to test and reason about, without sacrificing
the ease of rendering of vanilla Quil.

## Improved support for interactive programming in the REPL

When an exception is thrown from either `handle-event` or `draw`, the Inkwell
sketch becomes *paused*. While paused, neither `handle-event` nor `draw` are
called. This prevents you from being bombarded with sixty stack traces per
second when you make a mistake. When the mistake is fixed, call
`inkwell.core/resume!` to unpause the sketch.

Not only does Inkwell make sure to not flood you with stack traces, it makes
sure they are printed where you can see them. Quil prints exceptions to
`System/out`, whereas Inkwell redirects them to `*out*` to make sure they end
up in your REPL.

## Usage

Leiningen dependency information:

    [inkwell "0.1.0"]

To create a sketch, call `inkwell.core/make-sketch!` with a map of settings.
There are three keys in the settings map that Inkwell cares about:
`:handle-event` and `:draw`, whose values must be functions as described above,
and `:initial-state`, whose value must be the sketch's initial state (the value
first passed to the `:handle-event` and `:draw` functions).

The rest of the keys, such as `:title` or `:size`, are passed on to Quil.

## Events

The event parameter of `handle-event` may take one of the following values:

    {:type :tick} ; Sent after each `draw`

    {:type :mouse-moved
     :position [123 ; mouse x
                456 ; mouse y
    ]}

    {:type :mouse-pressed
     :button :left  ; or :center or :right
     :position [123 ; mouse x
                456 ; mouse y
    ]}

    {:type :mouse-released
     :button :left  ; or :center or :right
     :position [123 ; mouse x
                456 ; mouse y
    ]}

    {:type :key-pressed
     :key \c      ; from quil.core/raw-key
     :key-code 67 ; from quil.core/key-code
     :key-name :c ; from quil.core/key-as-keyword
    }

    {:type :key-released
     :key \c      ; from quil.core/raw-key
     :key-code 67 ; from quil.core/key-code
     :key-name :c ; from quil.core/key-as-keyword
    }

    {:type :mouse-wheel
     :direction :up ; or :down
    }

As you can see, for the most part, the event types correspond to Quil's event
type callbacks, and their values come directly from Quil's state lookup
functions.

## License

Copyright © 2014 [Solita](http://www.solita.fi)

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
