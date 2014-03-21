(ns inkwell.core-test
  (:require [midje.sweet :refer :all]
            [inkwell.core :refer :all]))

(fact "A new sketch starts out running"
  (with-open [s (sketch {})]
    @(:running? s) => true))

(facts "The draw function"
  (let [draw-count (atom 0)]
    (with-open [s (sketch {:draw #(swap! draw-count inc)})]

      (fact "is called while the sketch is running"
        (let [draw-count-before-sleep @draw-count]
          (Thread/sleep 100)
          (> @draw-count draw-count-before-sleep) => true))

      (fact "is not called while the sketch is stopped"
        (stop! s)
        (let [draw-count-before-sleep @draw-count]
          (Thread/sleep 100)
          (= @draw-count draw-count-before-sleep) => true)))))
