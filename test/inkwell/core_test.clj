(ns inkwell.core-test
  (:require [midje.sweet :refer :all]
            [inkwell.core :refer :all])
  (:import java.io.StringWriter))

(fact "A new sketch starts out running"
  (with-open [s (make-sketch! {})]
    @(:running? s) => true))

(facts "The draw function"
  (let [draw-count (atom 0)]
    (with-open [s (make-sketch! {:draw #(swap! draw-count inc)})]

      (fact "is called while the sketch is running"
        (let [draw-count-before-sleep @draw-count]
          (Thread/sleep 100)
          (> @draw-count draw-count-before-sleep) => true))

      (fact "is not called while the sketch is stopped"
        (stop! s)
        (let [draw-count-before-sleep @draw-count]
          (Thread/sleep 100)
          (= @draw-count draw-count-before-sleep) => true)))))

(facts "If the draw function throws an exception"
  (with-out-str
    (with-open [s (make-sketch! {:draw #(throw (Exception.))})]
      (Thread/sleep 100)

      (fact "the sketch is stopped"
        @(:running? s) => false)

      (fact "the exception stacktrace is printed"
        (str *out*) => #"java.lang.Exception"))))
