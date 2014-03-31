(ns inkwell.core
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.unsafe :as tu]
            quil.core)
  (:import (java.io StringWriter
                    PrintWriter)))

(t/def-alias QuilSketch quil.Applet)
(t/def-alias MouseButton (U ':left ':center ':right))

(t/ann ^:no-check quil.core/sketch [Any * -> quil.Applet])
(t/ann ^:no-check quil.core/sketch-close [QuilSketch -> Any])
(t/ann ^:no-check quil.core/mouse-x [-> t/AnyInteger])
(t/ann ^:no-check quil.core/mouse-y [-> t/AnyInteger])
(t/ann ^:no-check quil.core/mouse-button [-> MouseButton])
(t/ann ^:no-check quil.core/raw-key [-> Character])
(t/ann ^:no-check quil.core/key-code [-> t/AnyInteger])
(t/ann ^:no-check quil.core/key-as-keyword [-> t/Keyword])

(t/ann-record [State] InkwellSketch [quil-sketch :- QuilSketch
                                     paused? :- (t/Atom1 Boolean)
                                     state :- (t/Atom1 State)])
(defrecord InkwellSketch [quil-sketch paused? state]
  java.lang.AutoCloseable
  (close [sketch]
    (quil.core/sketch-close (:quil-sketch sketch))
    nil))

(t/def-alias Position (Vector* t/AnyInteger t/AnyInteger))

(t/def-alias TickEvent (HMap :mandatory {:type ':tick}))
(t/def-alias MouseMovedEvent (HMap :mandatory {:type ':mouse-moved
                                               :position Position}))
(t/def-alias MousePressedEvent (HMap :mandatory {:type ':mouse-pressed
                                                 :position Position
                                                 :button MouseButton}))
(t/def-alias MouseReleasedEvent (HMap :mandatory {:type ':mouse-released
                                                  :position Position
                                                  :button MouseButton}))
(t/def-alias MouseWheelEvent (HMap :mandatory {:type ':mouse-wheel
                                               :direction (U ':up ':down)}))
(t/def-alias KeyPressedEvent (HMap :mandatory {:type ':key-pressed
                                               :key Character
                                               :key-code t/AnyInteger
                                               :key-name t/Keyword}))
(t/def-alias KeyReleasedEvent (HMap :mandatory {:type ':key-released
                                                :key Character
                                                :key-code t/AnyInteger
                                                :key-name t/Keyword}))
(t/def-alias Event (U TickEvent
                      MouseMovedEvent
                      MousePressedEvent
                      MouseReleasedEvent
                      MouseWheelEvent
                      KeyPressedEvent
                      KeyReleasedEvent))

(t/def-alias Settings (TFn [[State :variance :invariant]]
                        (HMap :mandatory {:draw [State -> Any]
                                          :handle-event [State Event -> State]
                                          :initial-state State})))

(t/non-nil-return java.io.StringWriter/toString :all)

(t/ann throwable->string [Throwable -> String])
(defn throwable->string [^Throwable t]
  (let [string-writer (StringWriter.)]
    (.printStackTrace t (PrintWriter. string-writer))
    (.toString string-writer)))

(t/def-alias InkwellSketchMap (TFn [[State :variance :invariant]]
                                (HMap :mandatory {:paused? (t/Atom1 Boolean)
                                                  :main-thread-out java.io.Writer
                                                  :state (t/Atom1 State)
                                                  :settings (Settings State)})))

(t/ann event-adapter* (All [State]
                        [(InkwellSketchMap State) [Any * -> Event] -> [Any * -> Any]]))
(defn event-adapter*
  "Takes a fn that creates an Inkwell event, and returns a fn updates the
sketch's state with the users's `handle-event`."
  [sketch f]
  (fn [& args]
    (when-not @(:paused? sketch)
      (binding [*out* (:main-thread-out sketch)]
        (try
          (let [event (apply f args)
                handle-event (-> sketch :settings :handle-event)]
            (swap! (:state sketch) handle-event event))
          (catch Throwable t
            (println (throwable->string t))
            (reset! (:paused? sketch) true)))))))

(defmacro event-adapter [sketch & body]
  `(event-adapter* ~sketch ~(if (vector? (first body))
                              `(fn [& ~(first body)] ~@body)
                              `(fn [& _#] ~@body))))

(t/ann make-sketch! (All [State]
                      [(Settings State) -> (InkwellSketch State)]))
(defn make-sketch! [settings]
  (let [settings (merge {:draw (constantly nil)
                         :handle-event (fn [state _] state)}
                        settings)
        sketch {:paused? (t/atom> Boolean false)
                :main-thread-out *out*
                :state (atom (:initial-state settings))
                :settings settings}
        quil-settings (merge {:target :perm-frame}
                             settings
                             {:draw (event-adapter sketch
                                      ((:draw settings) @(:state sketch))
                                      {:type :tick})
                              :mouse-moved (event-adapter sketch
                                             {:type :mouse-moved
                                              :position [(quil.core/mouse-x)
                                                         (quil.core/mouse-y)]})
                              :mouse-pressed (event-adapter sketch
                                               {:type :mouse-pressed
                                                :button (quil.core/mouse-button)
                                                :position [(quil.core/mouse-x)
                                                           (quil.core/mouse-y)]})
                              :mouse-released (event-adapter sketch
                                                {:type :mouse-released
                                                 :button (quil.core/mouse-button)
                                                 :position [(quil.core/mouse-x)
                                                            (quil.core/mouse-y)]})
                              :mouse-wheel (event-adapter sketch [amount]
                                             (assert (number? amount))
                                             {:type :mouse-wheel
                                              :direction (if (neg? amount) :up :down)})
                              :key-pressed (event-adapter sketch
                                             {:type :key-pressed
                                              :key (quil.core/raw-key)
                                              :key-code (quil.core/key-code)
                                              :key-name (quil.core/key-as-keyword)})
                              :key-released (event-adapter sketch
                                              {:type :key-released
                                               :key (quil.core/raw-key)
                                               :key-code (quil.core/key-code)
                                               :key-name (quil.core/key-as-keyword)})})]
    (tu/ignore-with-unchecked-cast
      (map->InkwellSketch (-> sketch
                           (select-keys [:paused? :state])
                           (assoc :quil-sketch
                                  (apply quil.core/sketch (apply concat quil-settings)))))
      (InkwellSketch State))))

(t/ann pause! (All [State [x :< (InkwellSketch State)]]
                [x -> x]))
(defn pause! [sketch]
  (reset! (:paused? sketch) true)
  sketch)

(t/ann resume! (All [State [x :< (InkwellSketch State)]]
                 [x -> x]))
(defn resume! [sketch]
  (reset! (:paused? sketch) false)
  (reset! (:paused? sketch) false)
  sketch)
