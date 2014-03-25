(ns inkwell.core-test
  (:require [midje.sweet :refer :all]
            [inkwell.core :refer :all])
  (:import java.io.StringWriter))

(defn becomes [expected]
  (fn [f]
    (let [timeout-ms 100
          wait-ms 20
          start-ms (System/currentTimeMillis)
          end-ms (+ start-ms timeout-ms)]
      (loop []
        (or (= (f) expected)
            (when (< (System/currentTimeMillis) end-ms)
              (Thread/sleep wait-ms)
              (recur)))))))

(fact "A new sketch starts out running"
  (with-open [s (make-sketch! {})]
    @(:running? s) => true))

(facts "The draw function"
  (let [draw-count (atom 0)
        draw-arg (atom nil)]
    (with-open [s (make-sketch! {:draw (fn [state]
                                         (swap! draw-count inc)
                                         (reset! draw-arg state))
                                 :initial-state {:foo 0}})]

      (fact "is called while the sketch is running"
        (let [draw-count-before-sleep @draw-count]
          #(> @draw-count draw-count-before-sleep) => (becomes true)))

      (fact "is given the current state value as an argument"
        (swap! (:state s) update-in [:foo] inc)
        #(deref draw-arg) => (becomes {:foo 1}))

      (fact "is not called while the sketch is stopped"
        (stop! s)
        (let [draw-count-before-sleep @draw-count]
          #(= @draw-count draw-count-before-sleep) => (becomes true))))))

(facts "If the draw function throws an exception"
  (with-out-str
    (with-open [s (make-sketch! {:draw (fn [_] (throw (Exception.)))})]

      (fact "the sketch is stopped"
        #(deref (:running? s)) => (becomes false))

      (fact "the exception stacktrace is printed"
        (str *out*) => #"java.lang.Exception"))))
