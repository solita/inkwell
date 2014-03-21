(ns inkwell.core
  (:require [clojure.core.typed :as t]
            [quil.core :as q]))

(t/def-alias QuilSketch quil.Applet)

(t/ann ^:no-check quil.core/sketch [Any * -> quil.Applet])
(t/ann ^:no-check quil.core/sketch-close [QuilSketch -> Any])

(t/def-alias State (t/Map t/Keyword Any))

(defmacro state> [x]
  `(t/ann-form ~x State))

(t/ann-record InkwellSketch [quil-sketch :- QuilSketch
                             running? :- (t/Atom1 Boolean)
                             state :- (t/Agent1 State)])
(defrecord InkwellSketch [quil-sketch running? state]
  java.lang.AutoCloseable
  (close [sketch]
    (q/sketch-close (:quil-sketch sketch))
    nil))

(t/def-alias Settings (HMap :mandatory {:draw [-> Any]}))

(t/ann sketch [Settings -> InkwellSketch])
(defn sketch [settings]
  (let [running? (t/atom> Boolean true)]
    (t/letfn> [draw :- [-> Any]
      (draw []
        (when @running?
          (when-let [user-draw (:draw settings)]
            (user-draw))))]
      (map->InkwellSketch {:quil-sketch (q/sketch
                                          :draw draw)
                           :state (agent (state> {}))
                           :running? running?}))))

(t/ann stop! (All [[x :< InkwellSketch]] [x -> x]))
(defn stop! [sketch]
  (reset! (:running? (t/ann-form sketch InkwellSketch)) false)
  sketch)


