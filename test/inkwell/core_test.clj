(ns inkwell.core-test
  (:require [midje.sweet :refer :all]
            [midje.checking.core :refer [as-data-laden-falsehood
                                         data-laden-falsehood?]]
            [inkwell.core :refer :all])
  (:import java.io.StringWriter
           java.awt.Robot
           java.awt.event.InputEvent))

(defn becomes-like
  ([predicate]
    (becomes-like predicate {}))
  ([predicate options]
    (fn [f]
      (let [timeout-ms (:timeout options 100)
            wait-ms (:wait options 20)
            start-ms (System/currentTimeMillis)
            end-ms (+ start-ms timeout-ms)]
        (loop []
          (let [actual (f)]
            (or (predicate actual)
                (if (< (System/currentTimeMillis) end-ms)
                  (do
                    (Thread/sleep wait-ms)
                    (recur))
                  (as-data-laden-falsehood {:notes [(str "actual: " (pr-str actual))]})))))))))

(defn becomes
  ([expected]
    (becomes expected {}))
  ([expected options]
    (becomes-like (fn [actual]
                    (= actual (if (fn? expected)
                                (expected)
                                expected)))
                  options)))

(fact "A new sketch starts out running"
  (with-open [s (make-sketch! {})]
    @(:paused? s) => false))

(facts "The draw function"
  (let [draw-count (atom 0)
        draw-args (atom nil)]
    (with-open [s (make-sketch! {:draw (fn [& args]
                                         (swap! draw-count inc)
                                         (reset! draw-args args))
                                 :initial-state {:foo 0}})]

      (fact "is called while the sketch is running"
        (let [old-draw-count @draw-count]
          #(> @draw-count old-draw-count) => (becomes true)))

      (fact "is given the current state value as an argument"
        (swap! (:state s) update-in [:foo] inc)
        #(deref draw-args) => (becomes [{:foo 1}]))

      (fact "is not called while the sketch is paused"
        (pause! s)
        (let [old-draw-count @draw-count]
          (= @draw-count old-draw-count) => true)))))

(facts "If the draw function throws an exception"
  (with-out-str
    (with-open [s (make-sketch! {:draw (fn [_] (throw (Exception.)))})]

      (fact "the sketch is stopped"
        #(deref (:paused? s)) => (becomes true))

      (fact "the exception stacktrace is printed"
        (str *out*) => #"java.lang.Exception"))))

(defn window-coordinate= [a b]
  "Window decorations affect the cursor positions reported by the event, so this
method is used to check that actual coordinates are within the right ballpark of
the expected coordinates."
  (< (Math/abs (- b a)) 25))

(facts "The event handler"
  (let [handle-return-value (atom 0)
        handle-args (atom ())]
    (with-open [s (make-sketch! {:handle-event (fn [& args]
                                                 (swap! handle-args conj args)
                                                 (swap! handle-return-value inc))})]
      (let [frame @(:target-obj (meta (:quil-sketch s)))
            [frame-x frame-y] ((juxt #(.x %) #(.y %)) (.getLocationOnScreen frame))
            robot (Robot.)
            focus-frame (fn focus-frame []
                          (.mouseMove robot (+ frame-x 100) (+ frame-y 100))
                          (.mousePress robot InputEvent/BUTTON1_MASK)
                          (.mouseRelease robot InputEvent/BUTTON1_MASK)
                          ;; Give the frame some time to receive focus
                          (Thread/sleep 100))]

        (fact "gets a :tick event before each screen update"
          #(second (first @handle-args)) => (becomes {:type :tick}))

        (fact "gets a :mouse-moved event when the mouse moves"
          (reset! handle-args ())
          (focus-frame)
          (.mouseMove robot (+ frame-x 212) (+ frame-y 254))
          #(deref handle-args)
          => (becomes-like
               (fn [actual]
                 (some (fn [[_ event]]
                         (and (= (:type event) :mouse-moved)
                              (window-coordinate= 212 (get-in event [:position 0]))
                              (window-coordinate= 254 (get-in event [:position 1]))))
                       actual))))

        (fact "its return value becomes the new state value"
          #(deref (:state s)) => (becomes #(deref handle-return-value)))))))
