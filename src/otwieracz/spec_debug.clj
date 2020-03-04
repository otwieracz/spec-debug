(ns otwieracz.spec-debug
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :refer [expound]]
            [clojure.tools.logging :as log]))

(def ^:private original-spec-valid? clojure.spec.alpha/valid?)

(def +expound-defaults+ {:show-valid-values? true})

(defn- wrap-expound-log-error
  ([options valid?-fn]
   (fn [& args]
     (or (apply valid?-fn args)
         (try
           (let [message (with-out-str
                           (expound (first args) (second args)
                                    (merge +expound-defaults+
                                           (:expound-options options))))
                 log-fn  (get options
                              :log-fn
                              (fn [message] (log/error message)))]
             (when log-fn
               (log-fn message))
             false)
           ;; Catch every Throwable just in case to make no excpetion is thrown
           ;; from the wrapping function
           (catch Throwable _ false))))))

(defn enable-spec-debug!
  "Globally enable debugging of `spec/valid?`. Optionally provide `options` for handler."
  ([options]
   (alter-var-root (var clojure.spec.alpha/valid?) (partial wrap-expound-log-error options))
   true)
  ([]
   (enable-spec-debug! nil)))

(defn disable-spec-debug!
  "Globally disable debugging of `spec/valid?`."
  []
  (alter-var-root (var clojure.spec.alpha/valid?) (constantly original-spec-valid?))
  false)