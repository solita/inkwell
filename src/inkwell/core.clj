(ns inkwell.core
  (:require [clojure.core.typed :as t]
            [quil.core :as q])
  (:import (java.io StringWriter
                    PrintWriter)))

(t/def-alias QuilSketch quil.Applet)

(t/ann ^:no-check quil.core/sketch [Any * -> quil.Applet])
(t/ann ^:no-check quil.core/sketch-close [QuilSketch -> Any])

(t/ann-record [State] InkwellSketch [quil-sketch :- QuilSketch
                                     running? :- (t/Atom1 Boolean)
                                     state :- (t/Atom1 State)])
(defrecord InkwellSketch [quil-sketch running? state]
  java.lang.AutoCloseable
  (close [sketch]
    (q/sketch-close (:quil-sketch sketch))
    nil))

(t/def-alias Settings (TFn [[State :variance :invariant]]
                        (HMap :mandatory {:draw [State -> Any]
                                          :initial-state State})))

(t/non-nil-return java.io.StringWriter/toString :all)

(t/ann throwable->string [Throwable -> String])
(defn throwable->string [^Throwable t]
  (let [string-writer (StringWriter.)]
    (.printStackTrace t (PrintWriter. string-writer))
    (.toString string-writer)))

(t/ann make-sketch! (All [State]
                      [(Settings State) -> (InkwellSketch State)]))
(defn make-sketch! [settings]
  (let [running? (t/atom> Boolean true)
        main-thread-out *out*
        state (atom (:initial-state settings))]
    (t/letfn> [draw :- [-> Any]
               (draw []
                 (when @running?
                   (try
                     (when-let [user-draw (:draw settings)]
                       (user-draw @state))
                     (catch Throwable t
                       (binding [*out* main-thread-out]
                         (println (throwable->string t)))
                       (reset! running? false)))))]
      (map->InkwellSketch {:quil-sketch (q/sketch
                                          :draw draw)
                           :state state
                           :running? running?}))))

(t/ann stop! (All [State [x :< (InkwellSketch State)]]
               [x -> x]))
(defn stop! [sketch]
  (reset! (:running? sketch) false)
  sketch)
