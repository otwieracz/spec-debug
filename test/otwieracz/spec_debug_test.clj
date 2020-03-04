(ns otwieracz.spec-debug-test
  (:require [clojure.test :refer [deftest is testing]]
            [otwieracz.spec-debug :as debug]
            [clojure.spec.alpha :as spec]))

(defonce message (atom ""))

(deftest loging-test
  (testing "Default logger"
    (debug/enable-spec-debug! {:log-fn print})
    (is (seq (with-out-str
               (spec/valid? string? 123))))
    (debug/disable-spec-debug!))
  (testing "Custom logger"
    (debug/enable-spec-debug! {:log-fn (fn [m] 
                                         (reset! message m))})
    (is (and (empty? (with-out-str
                      (spec/valid? string? 123)))
             (seq @message)))
    (reset! message "")
    (debug/disable-spec-debug!)))

(defmacro time-spent-ns [& body]
  `(let [start# (. java.lang.System (clojure.core/nanoTime))
         ret# (do ~@body)
         stop# (. java.lang.System (clojure.core/nanoTime))]
     (- stop# start#)))

(deftest performance-test
  (debug/disable-spec-debug!)
  (testing "performance impact <20% without error"
    (let [base (time-spent-ns (doall (repeatedly 5000 #(spec/valid? string? "123"))))
          logged (do (debug/enable-spec-debug!)
                     (time-spent-ns (doall (repeatedly 5000 #(spec/valid? string? "123")))))]
      (debug/disable-spec-debug!)
      (is (> 1.2 (float (/ logged base))))))
  (testing "performance overhead less than 5ms with default logger"
    (let [base (/ (time-spent-ns (doall (repeatedly 5000 #(spec/valid? string? 123))))
                  5000)
          logged (do (debug/enable-spec-debug!)
                     (/ (time-spent-ns  (doall (repeatedly 5000 #(spec/valid? string? 123))))
                        5000))]
      (debug/disable-spec-debug!)
      (int (- logged base))
      (is (> 5
             ;; convert 10ms to ns
             (/ (- logged base)
                (Math/pow 10 6)))))))