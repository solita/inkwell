(ns inkwell.core
  (:require [clojure.core.typed :as t]
            [quil.core :as q])
  (:import (java.io StringWriter
                    PrintWriter)))

(t/def-alias QuilSketch quil.Applet)

(t/ann ^:no-check quil.core/sketch [Any * -> quil.Applet])
(t/ann ^:no-check quil.core/sketch-close [QuilSketch -> Any])
(t/ann ^:no-check quil.core/mouse-x [-> t/AnyInteger])
(t/ann ^:no-check quil.core/mouse-y [-> t/AnyInteger])

(t/ann-record [State] InkwellSketch [quil-sketch :- QuilSketch
                                     running? :- (t/Atom1 Boolean)
                                     state :- (t/Atom1 State)])
(defrecord InkwellSketch [quil-sketch running? state]
  java.lang.AutoCloseable
  (close [sketch]
    (q/sketch-close (:quil-sketch sketch))
    nil))

(t/def-alias Position (Vector* t/AnyInteger t/AnyInteger))

(t/def-alias TickEvent (HMap :mandatory {:type ':tick}))
(t/def-alias MouseMovedEvent (HMap :mandatory {:type ':mouse-moved
                                               :position Position}))

(t/def-alias Event (U MouseMovedEvent TickEvent))

(t/def-alias Settings (TFn [[State :variance :invariant]]
                        (HMap :mandatory {:draw [State -> Any]
                                          :handle-event [State Event -> State]
                                          :initial-state State}
                              :optional {:title String})))

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
                   (binding [*out* main-thread-out]
                     (try
                       (when-let [handle-event (:handle-event settings)]
                         (swap! state handle-event {:type :tick}))
                       (when-let [user-draw (:draw settings)]
                         (user-draw @state))
                       (catch Throwable t
                         (println (throwable->string t))
                         (reset! running? false))))))
               mouse-moved :- [-> Any]
               (mouse-moved []
                 (when @running?
                   (binding [*out* main-thread-out]
                     (try
                       (when-let [handle-event (:handle-event settings)]
                         (swap! state handle-event {:type :mouse-moved
                                                    :position [(quil.core/mouse-x)
                                                               (quil.core/mouse-y)]}))
                       (catch Throwable t
                         (println (throwable->string t))
                         (reset! running? false))))))]
      (map->InkwellSketch {:quil-sketch (q/sketch
                                          :title (:title settings)
                                          :draw draw
                                          :mouse-moved mouse-moved
                                          :target :perm-frame)
                           :state state
                           :running? running?}))))

(t/ann stop! (All [State [x :< (InkwellSketch State)]]
               [x -> x]))
(defn stop! [sketch]
  (reset! (:running? sketch) false)
  sketch)

(t/ann start! (All [State [x :< (InkwellSketch State)]]
                [x -> x]))
(defn start! [sketch]
  (reset! (:running? sketch) true)
  sketch)
